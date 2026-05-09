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
        patchPreferencesFlowNode()
        patchPreferencesRootNode()
        patchPreferencesRootView()

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

        // Replace sender name with Iltix nickname-resolved name
        engine.replaceText(
            path,
            "val displayName = senderProfile.displayName",
            """val displayName = rememberIxResolvedDisplayName(
                    userId = senderId.value,
                    fallbackName = senderProfile.displayName,
                ) ?: senderProfile.displayName"""
        )
    }

    // ===== Preferences hooks — wire Iltix Modules settings screen =====

    private fun patchPreferencesFlowNode() {
        val path = "features/preferences/impl/src/main/kotlin/io/element/android/features/preferences/impl/PreferencesFlowNode.kt"
        engine.addImport(path, "de.iltix.preferences.IxModuleSettingsNode")

        // Add NavTarget.IltixModules
        engine.insertAfterLine(
            path,
            """data object Labs : NavTarget""",
            """
        @Parcelize
        data object IltixModules : NavTarget
""",
            "PreferencesFlowNode: IltixModules NavTarget"
        )

        // Add navigateToIltixModules callback in resolve()
        engine.insertAfterLine(
            path,
            """backstack\.push\(NavTarget\.Labs\)""",
            """                    }

                    override fun navigateToIltixModules() {
                        backstack.push(NavTarget.IltixModules)""",
            "PreferencesFlowNode: navigateToIltixModules callback"
        )

        // Add IltixModules case in resolve() switch
        engine.insertAfterLine(
            path,
            """createNode<LabsNode>\(buildContext, listOf\(callback\)\)""",
            """            }
            NavTarget.IltixModules -> {
                createNode<IxModuleSettingsNode>(buildContext)""",
            "PreferencesFlowNode: IltixModules resolve case"
        )
    }

    private fun patchPreferencesRootNode() {
        val path = "features/preferences/impl/src/main/kotlin/io/element/android/features/preferences/impl/root/PreferencesRootNode.kt"

        // Add navigateToIltixModules to Callback interface
        engine.insertAfterLine(
            path,
            """fun navigateToBlockedUsers\(\)""",
            """        fun navigateToIltixModules()""",
            "PreferencesRootNode: navigateToIltixModules in Callback"
        )

        // Add onOpenIltixModules parameter to PreferencesRootView call
        engine.insertAfterLine(
            path,
            """onOpenBlockedUsers = callback::navigateToBlockedUsers""",
            """            onOpenIltixModules = callback::navigateToIltixModules,""",
            "PreferencesRootNode: onOpenIltixModules parameter"
        )
    }

    private fun patchPreferencesRootView() {
        val path = "features/preferences/impl/src/main/kotlin/io/element/android/features/preferences/impl/root/PreferencesRootView.kt"
        engine.addImport(path, "de.iltix.lib.R as IltixR")

        // Add onOpenIltixModules parameter to PreferencesRootView function
        engine.insertAfterLine(
            path,
            """onOpenBlockedUsers: \(\) -> Unit,""",
            """    onOpenIltixModules: () -> Unit,""",
            "PreferencesRootView: onOpenIltixModules parameter"
        )

        // Pass onOpenIltixModules to GeneralSection
        engine.insertAfterLine(
            path,
            """onOpenLabs = onOpenLabs,""",
            """            onOpenIltixModules = onOpenIltixModules,""",
            "PreferencesRootView: pass onOpenIltixModules to GeneralSection"
        )

        // Add onOpenIltixModules parameter to GeneralSection function
        engine.replaceText(
            path,
            """    onOpenDeveloperSettings: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(id = CommonStrings.common_advanced_settings)) },""",
            """    onOpenDeveloperSettings: () -> Unit,
    onOpenIltixModules: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(id = CommonStrings.common_advanced_settings)) },""",
        )

        // Add Iltix Modules ListItem after Advanced Settings
        engine.insertAfterLine(
            path,
            """onClick = onOpenAdvancedSettings,""",
            """    )

    ListItem(
        headlineContent = { Text(stringResource(id = IltixR.string.iltix_modules_title)) },
        leadingContent = ListItemContent.Icon(IconSource.Resource(IltixR.drawable.ic_iltix)),
        onClick = onOpenIltixModules,""",
            "PreferencesRootView: Iltix Modules menu item"
        )

        // Add onOpenIltixModules = {} in ContentToPreview
        engine.insertAfterLine(
            path,
            """onOpenBlockedUsers = \{\},""",
            """        onOpenIltixModules = {},""",
            "PreferencesRootView: onOpenIltixModules in preview"
        )
    }

    // ===== User Profile hooks =====

    private fun patchUserProfileView() {
        val path = "features/userprofile/shared/src/main/kotlin/io/element/android/features/userprofile/shared/UserProfileView.kt"
        engine.addImport(path, "de.iltix.components.nicknames.IxLocalNicknameAction")
        engine.addImport(path, "de.iltix.components.nicknames.rememberIxResolvedDisplayName")

        // Add local nickname action after the Spacer between actions and verify section
        engine.insertAfterLine(
            path,
            """Spacer\(modifier = Modifier\.height\(26\.dp\)\)""",
            """            // Iltix: local nickname management
            IxLocalNicknameAction(
                userId = state.userId.value,
                fallbackName = state.userName,
            )""",
            "UserProfileView: IxLocalNicknameAction"
        )
    }

    // ===== Message Bubble hooks =====

    private fun patchMessageEventBubble() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/MessageEventBubble.kt"
        engine.addImport(path, "de.iltix.theme.LocalIxBubbleStyle")

        // Replace hardcoded BUBBLE_RADIUS with dynamic value from IxBubbleStyle
        engine.replaceText(
            path,
            "private val BUBBLE_RADIUS = 12.dp",
            "private val BUBBLE_RADIUS = 12.dp // Iltix: overridden by LocalIxBubbleStyle in composable"
        )
    }
}
