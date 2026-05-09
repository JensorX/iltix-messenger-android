/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.push

import android.content.ContentValues
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.Inject
import androidx.datastore.preferences.preferencesDataStore
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import de.iltix.lib.preferences.IxRoomMediaAutoDownloadStore
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.designsystem.utils.CommonDrawables
import io.element.android.libraries.push.impl.notifications.channels.SILENT_NOTIFICATION_CHANNEL_ID
import io.element.android.libraries.push.impl.R as PushR
import de.iltix.lib.R as IltixR
import io.element.android.libraries.push.impl.notifications.model.NotifiableMessageEvent
import io.element.android.libraries.push.impl.notifications.model.ResolvedPushEvent
import kotlinx.coroutines.flow.first
import timber.log.Timber

private const val PENDING_VIDEO_KEY_PREFIX = "pending_video_"
private const val PENDING_VIDEO_DELIMITER = "||"
private const val PENDING_DOWNLOADS_NOTIFICATION_ID = 73_001

private val Context.ixAutoDownloadHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "iltix_media_auto_download_history")

@Inject
class IxMediaAutoDownloadService(
    @ApplicationContext private val context: Context,
) {
    suspend fun handleResolvedResults(results: Map<*, Result<ResolvedPushEvent>>) {
        if (!isIltixBuild()) return
        if (!isGlobalEnabled()) return

        val onWifi = isOnWifi()
        processQueuedVideosIfPossible(onWifi)

        for ((_, result) in results) {
            val resolvedEvent = result.getOrNull() as? ResolvedPushEvent.Event ?: continue
            val event = resolvedEvent.notifiableEvent as? NotifiableMessageEvent ?: continue
            val mediaUri = event.imageUri ?: continue
            val mimeType = event.imageMimeType.orEmpty()
            if (!mimeType.startsWith("image/") && !mimeType.startsWith("video/")) continue
            if (wasProcessed(event.eventId.value)) continue
            if (!isRoomEnabled(event.roomId.value)) continue

            if (shouldQueueVideoUntilWifi(mimeType = mimeType, onWifi = onWifi)) {
                queueVideoForWifi(event)
                markProcessed(event.eventId.value)
                continue
            }

            if (shouldQueueVideoDueToMobileLimit(mimeType = mimeType, mediaUri = mediaUri.toString(), onWifi = onWifi)) {
                queueVideoForWifi(event)
                markProcessed(event.eventId.value)
                continue
            }

            if (!networkAllowsDownload(mimeType, mediaUri.toString(), onWifi)) continue

            val saved = saveToDownloads(event)
            if (saved) {
                markProcessed(event.eventId.value)
            }
        }

        updatePendingDownloadsNotification()
    }

    private fun isIltixBuild(): Boolean = context.packageName.contains("iltix")

    private suspend fun isGlobalEnabled(): Boolean {
        return IxPreferencesStore(context).settingFlow(IxPrefs.MEDIA_AUTO_DOWNLOAD).first()
    }

    private suspend fun isRoomEnabled(roomId: String): Boolean {
        return IxRoomMediaAutoDownloadStore(context).enabledFlow(roomId).first()
    }

    private suspend fun networkAllowsDownload(mimeType: String, mediaUri: String, onWifi: Boolean): Boolean {
        val store = IxPreferencesStore(context)
        val networkMode = store.settingFlow(IxPrefs.MEDIA_AUTO_DOWNLOAD_NETWORK_MODE).first()
        val mobileVideoLimitMb = store.settingFlow(IxPrefs.MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB).first()

        if (networkMode == "wifi_only" && !onWifi) {
            return false
        }

        if (mimeType.startsWith("video/")) {
            if (!onWifi) {
                val sizeBytes = approximateContentLength(mediaUri)
                if (sizeBytes > 0L && sizeBytes > mobileVideoLimitMb * 1024L * 1024L) {
                    return false
                }
            }
        }
        return true
    }

    private suspend fun shouldQueueVideoUntilWifi(mimeType: String, onWifi: Boolean): Boolean {
        if (!mimeType.startsWith("video/") || onWifi) return false
        return IxPreferencesStore(context).settingFlow(IxPrefs.MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY).first()
    }

    private suspend fun shouldQueueVideoDueToMobileLimit(mimeType: String, mediaUri: String, onWifi: Boolean): Boolean {
        if (!mimeType.startsWith("video/") || onWifi) return false

        val store = IxPreferencesStore(context)
        val networkMode = store.settingFlow(IxPrefs.MEDIA_AUTO_DOWNLOAD_NETWORK_MODE).first()
        if (networkMode == "wifi_only") return false

        val mobileVideoLimitMb = store.settingFlow(IxPrefs.MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB).first()
        val sizeBytes = approximateContentLength(mediaUri)
        if (sizeBytes <= 0L) return false
        return sizeBytes > mobileVideoLimitMb * 1024L * 1024L
    }

    private fun isOnWifi(): Boolean {
        val connectivityManager = context.getSystemService<ConnectivityManager>() ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun approximateContentLength(uriString: String): Long {
        return runCatching {
            val uri = android.net.Uri.parse(uriString)
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd -> afd.length } ?: -1L
        }.getOrElse {
            Timber.w(it, "Failed to determine media size for auto-download")
            -1L
        }
    }

    private suspend fun wasProcessed(eventId: String): Boolean {
        val key = booleanPreferencesKey("event_${eventId}")
        return context.ixAutoDownloadHistoryDataStore.data.first()[key] ?: false
    }

    private suspend fun markProcessed(eventId: String) {
        val key = booleanPreferencesKey("event_${eventId}")
        context.ixAutoDownloadHistoryDataStore.edit { preferences ->
            preferences[key] = true
        }
    }

    private suspend fun queueVideoForWifi(event: NotifiableMessageEvent) {
        val uri = event.imageUri?.toString() ?: return
        val mimeType = event.imageMimeType ?: return
        val queueKey = stringPreferencesKey("$PENDING_VIDEO_KEY_PREFIX${event.eventId.value}")
        val payload = listOf(uri, mimeType, event.timestamp.toString(), event.eventId.value)
            .joinToString(PENDING_VIDEO_DELIMITER)
        context.ixAutoDownloadHistoryDataStore.edit { preferences ->
            preferences[queueKey] = payload
        }
    }

    private suspend fun pendingQueueCount(): Int {
        return context.ixAutoDownloadHistoryDataStore.data.first().asMap()
            .count { (key, value) -> key.name.startsWith(PENDING_VIDEO_KEY_PREFIX) && value is String }
    }

    private suspend fun updatePendingDownloadsNotification() {
        val notificationManager = NotificationManagerCompat.from(context)
        val pendingCount = pendingQueueCount()

        if (pendingCount <= 0) {
            notificationManager.cancel(PENDING_DOWNLOADS_NOTIFICATION_ID)
            return
        }

        val title = context.getString(IltixR.string.iltix_pending_downloads_notification_title)
        val text = context.getString(IltixR.string.iltix_pending_downloads_notification_text, pendingCount)

        val notification = NotificationCompat.Builder(context, SILENT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(CommonDrawables.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()

        notificationManager.notify(PENDING_DOWNLOADS_NOTIFICATION_ID, notification)
    }

    private suspend fun processQueuedVideosIfPossible(onWifi: Boolean) {
        if (!onWifi) return
        val queueEntries = context.ixAutoDownloadHistoryDataStore.data.first().asMap()
            .filter { (key, value) -> key.name.startsWith(PENDING_VIDEO_KEY_PREFIX) && value is String }

        if (queueEntries.isEmpty()) return

        for ((key, value) in queueEntries) {
            val payload = value as? String ?: continue
            val parts = payload.split(PENDING_VIDEO_DELIMITER)
            if (parts.size < 4) {
                removeQueuedEntry(key.name)
                continue
            }
            val uriString = parts[0]
            val mimeType = parts[1]
            val timestamp = parts[2].toLongOrNull() ?: System.currentTimeMillis()
            val eventId = parts[3]

            val saved = saveUriToDownloads(uriString = uriString, mimeType = mimeType, timestamp = timestamp, eventId = eventId)
            if (saved) {
                removeQueuedEntry(key.name)
            }
        }
    }

    private suspend fun removeQueuedEntry(keyName: String) {
        context.ixAutoDownloadHistoryDataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(keyName))
        }
    }

    private fun saveToDownloads(event: NotifiableMessageEvent): Boolean {
        val uri = event.imageUri ?: return false
        val mimeType = event.imageMimeType ?: "application/octet-stream"
        return saveUriToDownloads(
            uriString = uri.toString(),
            mimeType = mimeType,
            timestamp = event.timestamp,
            eventId = event.eventId.value,
        )
    }

    private fun saveUriToDownloads(
        uriString: String,
        mimeType: String,
        timestamp: Long,
        eventId: String,
    ): Boolean {
        val uri = android.net.Uri.parse(uriString)
        val extension = when {
            mimeType.startsWith("image/") -> mimeType.substringAfter("image/")
            mimeType.startsWith("video/") -> mimeType.substringAfter("video/")
            else -> "bin"
        }
        val fileName = "${timestamp}_${eventId}.$extension"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Iltix")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val outputUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return false

        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                resolver.openOutputStream(outputUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(outputUri, contentValues, null, null)
            }
            true
        }.getOrElse { throwable ->
            Timber.e(throwable, "Failed to auto-save media for event $eventId")
            resolver.delete(outputUri, null, null)
            false
        }
    }

}
