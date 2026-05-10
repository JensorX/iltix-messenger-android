/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.nowbar

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import de.iltix.lib.R
import de.iltix.lib.preferences.IxBoolPref
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Shared Now Bar constants and gating for additive sidecar notifications.
 * Existing notifications remain unchanged.
 */
object IxNowBar {
    const val ONGOING_CALL_NOW_BAR_NOTIFICATION_ID = 970_001
    const val MEDIA_NOW_BAR_NOTIFICATION_ID = 970_002
    const val FAVORITE_CHAT_NOW_BAR_NOTIFICATION_ID = 970_003

    private const val CALL_CHANNEL_ID = "ix_now_bar_call_channel"
    private const val MEDIA_CHANNEL_ID = "ix_now_bar_media_channel"
    private const val FAVORITES_CHANNEL_ID = "ix_now_bar_favorites_channel"

    /**
     * Gate sidecar posting to Samsung devices only.
     */
    fun isEligibleSamsungDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase(Locale.ROOT).orEmpty()
        val brand = Build.BRAND?.lowercase(Locale.ROOT).orEmpty()
        return manufacturer.contains("samsung") || brand.contains("samsung")
    }

    fun postOngoingCallSidecar(
        context: Context,
        contentIntent: PendingIntent,
        title: String,
        text: String,
    ) {
        if (!isFeatureEnabled(context, IxPrefs.NOW_BAR_CALLS)) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CALL_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.iltix_now_bar_call_channel_title))
                .setDescription(context.getString(R.string.iltix_now_bar_call_channel_subtitle))
                .build()
        )
        val caller = Person.Builder()
            .setName(title)
            .setImportant(true)
            .build()
        val notification = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setStyle(NotificationCompat.CallStyle.forOngoingCall(caller, contentIntent))
            .build()
        manager.notify(ONGOING_CALL_NOW_BAR_NOTIFICATION_ID, notification)
    }

    fun clearOngoingCallSidecar(context: Context) {
        NotificationManagerCompat.from(context).cancel(ONGOING_CALL_NOW_BAR_NOTIFICATION_ID)
    }

    fun postMediaPlaybackSidecar(
        context: Context,
        mediaId: String,
        isPlaying: Boolean,
    ) {
        clearMediaPlaybackSidecar(context)
    }

    fun clearMediaPlaybackSidecar(context: Context) {
        NotificationManagerCompat.from(context).cancel(MEDIA_NOW_BAR_NOTIFICATION_ID)
    }

    fun postFavoriteChatHint(
        context: Context,
        roomName: String,
        unreadCount: Long,
    ) {
        clearFavoriteChatHint(context)
    }

    fun clearFavoriteChatHint(context: Context) {
        NotificationManagerCompat.from(context).cancel(FAVORITE_CHAT_NOW_BAR_NOTIFICATION_ID)
    }

    private fun isFeatureEnabled(context: Context, pref: IxBoolPref): Boolean {
        if (!isEligibleSamsungDevice()) return false
        val prefs = IxPreferencesStore(context.applicationContext)
        val nowBarEnabled = runCatching {
            runBlocking {
                withTimeoutOrNull(1_000L) { prefs.settingFlow(IxPrefs.NOW_BAR_ENABLED).first() }
            }
        }.getOrNull() ?: IxPrefs.NOW_BAR_ENABLED.defaultValue
        if (!nowBarEnabled) return false
        return runCatching {
            runBlocking {
                withTimeoutOrNull(1_000L) { prefs.settingFlow(pref).first() }
            }
        }.getOrNull() ?: pref.defaultValue
    }
}
