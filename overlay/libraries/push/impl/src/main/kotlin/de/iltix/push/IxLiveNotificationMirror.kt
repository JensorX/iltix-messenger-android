/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.push

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.designsystem.utils.CommonDrawables
import io.element.android.libraries.matrix.api.timeline.item.event.EventType
import io.element.android.libraries.push.impl.notifications.RoomEventGroupInfo
import io.element.android.libraries.push.impl.notifications.model.NotifiableMessageEvent
import io.element.android.libraries.push.impl.notifications.shortcut.createShortcutId
import io.element.android.services.toolbox.api.strings.StringProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import de.iltix.lib.R as IltixR

internal const val IX_LIVE_NOTIFICATION_CHANNEL_ID = "ILTIX_LIVE_NOTIFICATIONS"

private const val IX_LIVE_NOTIFICATION_ID = 74_001
private const val IX_LIVE_NOTIFICATION_TAG_PREFIX = "iltix_live:"
private const val SHORT_CRITICAL_TEXT_MAX = 28

internal fun ensureIxLiveNotificationChannel(
    context: Context,
    notificationManager: NotificationManagerCompat,
    stringProvider: StringProvider,
) {
    if (!context.packageName.contains("iltix")) return
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (notificationManager.getNotificationChannel(IX_LIVE_NOTIFICATION_CHANNEL_ID) != null) return

    notificationManager.createNotificationChannel(
        NotificationChannelCompat.Builder(
            IX_LIVE_NOTIFICATION_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        )
            .setName(stringProvider.getString(IltixR.string.iltix_live_notifications_title))
            .setDescription(stringProvider.getString(IltixR.string.iltix_live_notifications_subtitle))
            .setSound(null, null)
            .setVibrationEnabled(false)
            .setLightsEnabled(false)
            .build(),
    )
}

internal fun publishIxLiveNotification(
    context: Context,
    buildMeta: BuildMeta,
    roomInfo: RoomEventGroupInfo,
    latestEvent: NotifiableMessageEvent,
    normalNotification: Notification,
) {
    if (!buildMeta.applicationId.contains("iltix")) return
    val mirrorKey = liveNotificationKey(roomInfo)
    if (!latestEvent.shouldCreateLiveMirror()) {
        cancelIxLiveNotification(context, mirrorKey)
        return
    }
    if (!isIxLiveNotificationsEnabled(context)) {
        cancelIxLiveNotification(context, mirrorKey)
        return
    }

    val notification = buildIxLiveNotification(
        context = context,
        buildMeta = buildMeta,
        roomInfo = roomInfo,
        latestEvent = latestEvent,
        normalNotification = normalNotification,
    )

    runCatching {
        NotificationManagerCompat.from(context)
            .notify(liveNotificationTag(mirrorKey), IX_LIVE_NOTIFICATION_ID, notification)
    }.onFailure { throwable ->
        Timber.w(throwable, "Failed to publish Iltix live notification for room ${roomInfo.roomId}")
    }
}

internal fun syncIxLiveNotificationMirrors(
    context: Context,
    activeRoomKeys: Set<String>,
) {
    if (!context.packageName.contains("iltix")) return
    if (!isIxLiveNotificationsEnabled(context)) {
        cancelAllIxLiveNotifications(context)
        return
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

    val activeTags = activeRoomKeys.map(::liveNotificationTag).toSet()
    val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
    val compatManager = NotificationManagerCompat.from(context)
    notificationManager.activeNotifications
        .filter { it.id == IX_LIVE_NOTIFICATION_ID }
        .mapNotNull { it.tag }
        .filter { it.startsWith(IX_LIVE_NOTIFICATION_TAG_PREFIX) && it !in activeTags }
        .forEach { tag -> compatManager.cancel(tag, IX_LIVE_NOTIFICATION_ID) }
}

fun cancelAllIxLiveNotifications(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
    val compatManager = NotificationManagerCompat.from(context)
    notificationManager.activeNotifications
        .filter { it.id == IX_LIVE_NOTIFICATION_ID }
        .mapNotNull { it.tag }
        .filter { it.startsWith(IX_LIVE_NOTIFICATION_TAG_PREFIX) }
        .forEach { tag -> compatManager.cancel(tag, IX_LIVE_NOTIFICATION_ID) }
}

internal fun ixLiveNotificationKey(roomInfo: RoomEventGroupInfo): String = liveNotificationKey(roomInfo)

internal fun ixLiveNotificationKey(event: NotifiableMessageEvent): String {
    return "${event.sessionId.value}|${event.roomId.value}"
}

private fun buildIxLiveNotification(
    context: Context,
    buildMeta: BuildMeta,
    roomInfo: RoomEventGroupInfo,
    latestEvent: NotifiableMessageEvent,
    normalNotification: Notification,
): Notification {
    val roomTitle = roomInfo.roomDisplayName.ifBlank { buildMeta.applicationName }
    val senderName = latestEvent.senderDisambiguatedDisplayName?.trim().orEmpty()
    val contentText = when {
        roomInfo.isDm -> buildMeta.applicationName
        senderName.isNotBlank() -> senderName
        else -> buildMeta.applicationName
    }
    val shortText = if (roomInfo.isDm && senderName.isNotBlank()) {
        senderName
    } else {
        roomTitle
    }.limitShortCriticalText()

    return NotificationCompat.Builder(context, IX_LIVE_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(CommonDrawables.ic_notification)
        .setContentTitle(roomTitle)
        .setContentText(contentText)
        .setSubText(buildMeta.applicationName)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setDefaults(0)
        .setOngoing(true)
        .setAutoCancel(false)
        .setWhen(latestEvent.timestamp)
        .setShowWhen(false)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setShortcutId(createShortcutId(roomInfo.sessionId, roomInfo.roomId))
        .apply {
            normalNotification.contentIntent?.let(::setContentIntent)
        }
        .setRequestPromotedOngoing(true)
        .setShortCriticalText(shortText)
        .build()
}

private fun isIxLiveNotificationsEnabled(context: Context): Boolean {
    return runCatching {
        runBlocking {
            withTimeoutOrNull(2_000L) {
                IxPreferencesStore(context).settingFlow(IxPrefs.LIVE_NOTIFICATIONS).first()
            }
        }
    }.getOrElse { throwable ->
        Timber.w(throwable, "Failed to read Iltix live notification preference")
        IxPrefs.LIVE_NOTIFICATIONS.defaultValue
    } ?: IxPrefs.LIVE_NOTIFICATIONS.defaultValue
}

private fun NotifiableMessageEvent.shouldCreateLiveMirror(): Boolean {
    return !outGoingMessage && type != EventType.RTC_NOTIFICATION
}

private fun liveNotificationKey(roomInfo: RoomEventGroupInfo): String {
    return "${roomInfo.sessionId.value}|${roomInfo.roomId.value}"
}

private fun liveNotificationTag(mirrorKey: String): String {
    return "$IX_LIVE_NOTIFICATION_TAG_PREFIX$mirrorKey"
}

private fun cancelIxLiveNotification(context: Context, mirrorKey: String) {
    NotificationManagerCompat.from(context).cancel(liveNotificationTag(mirrorKey), IX_LIVE_NOTIFICATION_ID)
}

private fun String.limitShortCriticalText(): String {
    val normalized = trim().replace(Regex("\\s+"), " ")
    return if (normalized.length <= SHORT_CRITICAL_TEXT_MAX) {
        normalized
    } else {
        normalized.take(SHORT_CRITICAL_TEXT_MAX - 3).trimEnd() + "..."
    }
}
