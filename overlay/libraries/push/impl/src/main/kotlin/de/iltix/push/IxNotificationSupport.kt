package de.iltix.push

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_NOTIFICATION
import android.net.Uri
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.iltix.lib.nicknames.IxLocalNicknameStore
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.push.impl.R
import io.element.android.libraries.push.impl.notifications.RoomEventGroupInfo
import io.element.android.libraries.push.impl.notifications.channels.NotificationChannels
import io.element.android.libraries.push.impl.notifications.model.NotifiableMessageEvent
import io.element.android.services.toolbox.api.strings.StringProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

internal const val IX_PRIORITY_SILENT_NOTIFICATION_CHANNEL_ID = "IX_PRIORITY_SILENT_NOTIFICATION_CHANNEL_ID"
internal const val IX_PRIORITY_NOISY_NOTIFICATION_CHANNEL_ID = "IX_PRIORITY_NOISY_NOTIFICATION_CHANNEL_ID"

internal data class IxNotificationRoute(
    val channelId: String,
    val priority: Int,
    val shouldSetLights: Boolean,
)

internal fun resolveIxRankingTimestamp(
    baseTimestamp: Long,
    ixRoute: IxNotificationRoute?,
): Long {
    return if (ixRoute != null) System.currentTimeMillis() else baseTimestamp
}

internal suspend fun resolveIxNotificationRoute(
    context: Context,
    buildMeta: BuildMeta,
    roomInfo: RoomEventGroupInfo,
): IxNotificationRoute? {
    if (!buildMeta.applicationId.contains("iltix")) return null

    val priorityNotificationEnabled = runCatching {
        IxPreferencesStore(context).settingFlow(IxPrefs.PRIORITY_NOTIFICATION).first()
    }.getOrDefault(IxPrefs.PRIORITY_NOTIFICATION.defaultValue)
    if (!priorityNotificationEnabled) return null

    return IxNotificationRoute(
        // Use the noisy high-importance channel for all priority notifications so Android can show heads-up reliably.
        channelId = IX_PRIORITY_NOISY_NOTIFICATION_CHANNEL_ID,
        priority = NotificationCompat.PRIORITY_MAX,
        shouldSetLights = true,
    )
}

internal fun resolveIxSummaryNotificationRoute(
    context: Context,
    buildMeta: BuildMeta,
): IxNotificationRoute? {
    if (!buildMeta.applicationId.contains("iltix")) return null

    // runBlocking is used here because the call-chain (SummaryGroupMessageCreator →
    // NotificationCreator) does not support suspend. The DataStore singleton caches values
    // after first access, so this normally completes instantly. The timeout guards against
    // cold-start edge cases to prevent ANR.
    val priorityNotificationEnabled = runCatching {
        runBlocking {
            withTimeoutOrNull(2_000L) {
                IxPreferencesStore(context).settingFlow(IxPrefs.PRIORITY_NOTIFICATION).first()
            }
        }
    }.getOrNull() ?: IxPrefs.PRIORITY_NOTIFICATION.defaultValue
    if (!priorityNotificationEnabled) return null

    return IxNotificationRoute(
        channelId = IX_PRIORITY_NOISY_NOTIFICATION_CHANNEL_ID,
        priority = NotificationCompat.PRIORITY_MAX,
        shouldSetLights = true,
    )
}

internal suspend fun resolveIxNotificationSenderName(
    context: Context,
    buildMeta: BuildMeta,
    event: NotifiableMessageEvent,
): String {
    val fallbackName = event.senderDisambiguatedDisplayName.orEmpty()
    if (!buildMeta.applicationId.contains("iltix")) {
        return fallbackName
    }

    val localUsernamesEnabled = IxPreferencesStore(context).settingFlow(IxPrefs.LOCAL_USERNAMES).first()
    if (!localUsernamesEnabled) return fallbackName

    return IxLocalNicknameStore(context).nicknameFlow(event.senderId.value).first()
        ?: fallbackName
}

internal fun createIxPriorityNotificationChannels(
    context: Context,
    notificationManager: NotificationManagerCompat,
    stringProvider: StringProvider,
    accentColor: Int,
) {
    if (!context.packageName.contains("iltix")) return

    val existingChannels = notificationManager.notificationChannels.associateBy { it.id }

    if (existingChannels[IX_PRIORITY_NOISY_NOTIFICATION_CHANNEL_ID] == null) {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                IX_PRIORITY_NOISY_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_HIGH,
            )
                .setSound(
                    Uri.Builder()
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .path("//" + context.packageName + "/" + R.raw.message)
                        .build(),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(USAGE_NOTIFICATION)
                        .build(),
                )
                .setName("Iltix Benachrichtigungen")
                .setDescription("Priorisierte Iltix-Benachrichtigungen")
                .setVibrationEnabled(true)
                .setLightsEnabled(true)
                .setLightColor(accentColor)
                .build(),
        )
    }

    if (existingChannels[IX_PRIORITY_SILENT_NOTIFICATION_CHANNEL_ID] == null) {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                IX_PRIORITY_SILENT_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_HIGH,
            )
                .setName("Iltix Benachrichtigungen (Stumm)")
                .setDescription("Priorisierte stumme Iltix-Benachrichtigungen")
                .setSound(null, null)
                .setVibrationEnabled(false)
                .setLightsEnabled(true)
                .setLightColor(accentColor)
                .build(),
        )
    }
}