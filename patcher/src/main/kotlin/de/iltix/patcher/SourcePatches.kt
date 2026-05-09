package de.iltix.patcher

import java.io.File

/**
 * Applies all source code patches (Kotlin files) to hook Iltix into upstream Element X.
 * Each method patches one upstream file with the required Iltix imports and code insertions.
 *
 * IMPORTANT: All patterns use regex anchors against stable upstream code structures,
 * never line numbers. If upstream refactors a file significantly, the patch will fail
 * loudly (PatchVerifier catches it) rather than silently corrupting code.
 */
class SourcePatches(private val engine: PatchEngine) {

    fun applyAll() {
        println("\n--- Applying source patches ---")
        patchMainActivity()
        patchLoggedInFlowNode()
        patchHomeView()
        patchRoomListPresenter()
        patchRoomSummaryRow()
        patchRoomListContentView()
        patchMessagesView()
        patchMessageComposerView()
        patchMessagesViewTopBar()
        patchThreadTopBar()
        patchTimelineItemEventRow()
        patchMessageEventBubble()
        patchTimelineItemVoiceView()
        patchTimelineItemPollView()
        patchRoomDetailsPresenter()
        patchRoomDetailsView()
        patchUserProfileView()
        patchNotificationCreator()
        patchNotificationChannels()
        patchFetchPendingNotificationsWorker()
        patchSenderName()

        val results = engine.getResults()
        val failures = engine.failedResults()
        println("  Source patches: ${results.size - failures.size} succeeded, ${failures.size} failed")
        if (failures.isNotEmpty()) {
            failures.forEach { println("    ✗ ${it.file}: ${it.operation} — ${it.message}") }
        }
    }

    // ===== Theme hooks =====

    private fun patchMainActivity() {
        val path = "app/src/main/kotlin/io/element/android/x/MainActivity.kt"
        engine.addImport(path, "de.iltix.theme.IxElementThemeApp")
        engine.replaceText(path, "ElementThemeApp(", "IxElementThemeApp(")
    }

    private fun patchLoggedInFlowNode() {
        val path = "appnav/src/main/kotlin/io/element/android/appnav/LoggedInFlowNode.kt"
        engine.addImport(path, "de.iltix.theme.IxElementThemeApp")
        engine.replaceText(path, "ElementThemeApp(", "IxElementThemeApp(")
    }

    // ===== Home hooks =====

    private fun patchHomeView() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/HomeView.kt"
        engine.addImport(path, "de.iltix.home.IxHomeChatsContent")
        engine.addImport(path, "de.iltix.home.rememberIxHomeUiConfig")

        // Insert IxHomeUiConfig initialization after the roomListState declaration
        engine.insertAfterLine(
            path,
            """val roomListState.*=.*\.roomListState""",
            """
    val ixHomeUi = rememberIxHomeUiConfig(
        currentHomeNavigationBarItem = state.currentHomeNavigationBarItem,
        roomListState = roomListState,
    )
    val showBottomBar = !ixHomeUi.shouldShowIxSpaceNav""",
            "HomeView: ixHomeUi initialization"
        )
    }

    private fun patchRoomListPresenter() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/roomlist/RoomListPresenter.kt"
        engine.addImport(path, "de.iltix.home.IxRoomPrefsSource")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
    }

    private fun patchRoomSummaryRow() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/components/RoomSummaryRow.kt"
        engine.addImport(path, "de.iltix.components.badges.IxUnreadBadge")
        engine.addImport(path, "de.iltix.components.nicknames.rememberIxResolvedDisplayName")
        engine.addImport(path, "de.iltix.components.roomlist.IxFavoriteStarIcon")
        engine.addImport(path, "de.iltix.home.rememberIxRoomSummaryConfig")
    }

    private fun patchRoomListContentView() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/components/RoomListContentView.kt"
        // The RoomListContentView uses fully-qualified references, so we don't need imports
        // but we need to verify the IxPreferencesStore usage pattern can be inserted
    }

    // ===== Messages hooks =====

    private fun patchMessagesView() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/MessagesView.kt"
        engine.addImport(path, "de.iltix.messages.IxEmojiKeyboardPanel")
        engine.addImport(path, "de.iltix.messages.rememberIxEmojiPanelState")
    }

    private fun patchMessageComposerView() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/messagecomposer/MessageComposerView.kt"
        engine.addImport(path, "de.iltix.lib.preferences.IxPreferencesStore")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
    }

    private fun patchMessagesViewTopBar() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/topbars/MessagesViewTopBar.kt"
        engine.addImport(path, "de.iltix.components.nicknames.rememberIxResolvedDisplayName")
        engine.addImport(path, "de.iltix.lib.preferences.IxPreferencesStore")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
    }

    private fun patchThreadTopBar() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/topbars/ThreadTopBar.kt"
        engine.addImport(path, "de.iltix.components.nicknames.rememberIxResolvedDisplayName")
        engine.addImport(path, "de.iltix.lib.preferences.IxPreferencesStore")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
    }

    private fun patchTimelineItemEventRow() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/TimelineItemEventRow.kt"
        engine.addImport(path, "de.iltix.theme.LocalIxBubbleStyle")
    }

    private fun patchMessageEventBubble() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/MessageEventBubble.kt"
        engine.addImport(path, "de.iltix.theme.LocalIxBubbleStyle")
    }

    private fun patchTimelineItemVoiceView() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/event/TimelineItemVoiceView.kt"
        engine.addImport(path, "de.iltix.messages.IxVoiceMessageBody")
        engine.addImport(path, "de.iltix.theme.LocalIxBubbleStyle")
        engine.addImport(path, "de.iltix.messages.IxVoiceMessageView")
        engine.addImport(path, "de.iltix.messages.rememberIxVoiceMessageUiConfig")
    }

    private fun patchTimelineItemPollView() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/event/TimelineItemPollView.kt"
        engine.addImport(path, "de.iltix.components.poll.IxPollVoteViewerSheet")
        engine.addImport(path, "de.iltix.lib.R as IltixR")
        engine.addImport(path, "de.iltix.lib.preferences.IxPreferencesStore")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
    }

    // ===== Room Details hooks =====

    private fun patchRoomDetailsPresenter() {
        val path = "features/roomdetails/impl/src/main/kotlin/io/element/android/features/roomdetails/impl/RoomDetailsPresenter.kt"
        engine.addImport(path, "de.iltix.lib.preferences.IxPreferencesStore")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
        engine.addImport(path, "de.iltix.lib.preferences.IxRoomMediaAutoDownloadStore")
    }

    private fun patchRoomDetailsView() {
        val path = "features/roomdetails/impl/src/main/kotlin/io/element/android/features/roomdetails/impl/RoomDetailsView.kt"
        engine.addImport(path, "de.iltix.lib.R as IltixR")
    }

    // ===== User Profile hooks =====

    private fun patchUserProfileView() {
        val path = "features/userprofile/shared/src/main/kotlin/io/element/android/features/userprofile/shared/UserProfileView.kt"
        engine.addImport(path, "de.iltix.components.nicknames.IxLocalNicknameAction")
        engine.addImport(path, "de.iltix.components.nicknames.rememberIxResolvedDisplayName")
    }

    // ===== Push/Notification hooks =====

    private fun patchNotificationCreator() {
        val path = "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/notifications/factories/NotificationCreator.kt"
        engine.addImport(path, "de.iltix.push.resolveIxNotificationRoute")
        engine.addImport(path, "de.iltix.push.resolveIxRankingTimestamp")
        engine.addImport(path, "de.iltix.push.resolveIxSummaryNotificationRoute")
        engine.addImport(path, "de.iltix.push.resolveIxNotificationSenderName")
    }

    private fun patchNotificationChannels() {
        val path = "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/notifications/channels/NotificationChannels.kt"
        engine.addImport(path, "de.iltix.push.createIxPriorityNotificationChannels")
    }

    private fun patchFetchPendingNotificationsWorker() {
        val path = "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/workmanager/FetchPendingNotificationsWorker.kt"
        engine.addImport(path, "de.iltix.push.IxMediaAutoDownloadService")
    }

    // ===== Matrix UI hooks =====

    private fun patchSenderName() {
        val path = "libraries/matrixui/src/main/kotlin/io/element/android/libraries/matrix/ui/messages/sender/SenderName.kt"
        engine.addImport(path, "de.iltix.components.nicknames.rememberIxResolvedDisplayName")
    }
}
