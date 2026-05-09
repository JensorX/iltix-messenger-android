package de.iltix.patcher

import java.io.File

/**
 * Verifies that all patches were applied correctly by checking
 * for expected symbols in the patched workspace files.
 */
class PatchVerifier(private val workspace: File) {

    data class Expectation(
        val file: String,
        val expectedContent: String,
        val description: String,
    )

    data class VerificationResult(
        val expectation: Expectation,
        val passed: Boolean,
        val message: String,
    )

    /**
     * All expected symbols/imports that must exist after patching.
     */
    fun allExpectations(): List<Expectation> = buildList {
        // === Theme hooks ===
        add(Expectation(
            "app/src/main/kotlin/io/element/android/x/MainActivity.kt",
            "import de.iltix.theme.IxElementThemeApp",
            "MainActivity: IxElementThemeApp import"
        ))
        add(Expectation(
            "app/src/main/kotlin/io/element/android/x/MainActivity.kt",
            "IxElementThemeApp(",
            "MainActivity: IxElementThemeApp call"
        ))
        add(Expectation(
            "appnav/src/main/kotlin/io/element/android/appnav/LoggedInFlowNode.kt",
            "import de.iltix.theme.IxElementThemeApp",
            "LoggedInFlowNode: IxElementThemeApp import"
        ))

        // === Home hooks ===
        add(Expectation(
            "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/HomeView.kt",
            "import de.iltix.home.IxHomeChatsContent",
            "HomeView: IxHomeChatsContent import"
        ))
        add(Expectation(
            "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/HomeView.kt",
            "import de.iltix.home.rememberIxHomeUiConfig",
            "HomeView: rememberIxHomeUiConfig import"
        ))
        add(Expectation(
            "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/roomlist/RoomListPresenter.kt",
            "import de.iltix.home.IxRoomPrefsSource",
            "RoomListPresenter: IxRoomPrefsSource import"
        ))
        add(Expectation(
            "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/components/RoomSummaryRow.kt",
            "import de.iltix.components.badges.IxUnreadBadge",
            "RoomSummaryRow: IxUnreadBadge import"
        ))
        add(Expectation(
            "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/components/RoomSummaryRow.kt",
            "import de.iltix.components.nicknames.rememberIxResolvedDisplayName",
            "RoomSummaryRow: rememberIxResolvedDisplayName import"
        ))

        // === Messages hooks ===
        add(Expectation(
            "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/MessagesView.kt",
            "import de.iltix.messages.IxEmojiKeyboardPanel",
            "MessagesView: IxEmojiKeyboardPanel import"
        ))
        add(Expectation(
            "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/messagecomposer/MessageComposerView.kt",
            "import de.iltix.lib.preferences.IxPreferencesStore",
            "MessageComposerView: IxPreferencesStore import"
        ))
        add(Expectation(
            "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/topbars/MessagesViewTopBar.kt",
            "import de.iltix.components.nicknames.rememberIxResolvedDisplayName",
            "MessagesViewTopBar: rememberIxResolvedDisplayName import"
        ))
        add(Expectation(
            "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/TimelineItemEventRow.kt",
            "import de.iltix.theme.LocalIxBubbleStyle",
            "TimelineItemEventRow: LocalIxBubbleStyle import"
        ))
        add(Expectation(
            "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/MessageEventBubble.kt",
            "import de.iltix.theme.LocalIxBubbleStyle",
            "MessageEventBubble: LocalIxBubbleStyle import"
        ))
        add(Expectation(
            "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/event/TimelineItemVoiceView.kt",
            "import de.iltix.messages.IxVoiceMessageBody",
            "TimelineItemVoiceView: IxVoiceMessageBody import"
        ))
        add(Expectation(
            "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/event/TimelineItemPollView.kt",
            "import de.iltix.components.poll.IxPollVoteViewerSheet",
            "TimelineItemPollView: IxPollVoteViewerSheet import"
        ))

        // === Room Details hooks ===
        add(Expectation(
            "features/roomdetails/impl/src/main/kotlin/io/element/android/features/roomdetails/impl/RoomDetailsPresenter.kt",
            "import de.iltix.lib.preferences.IxPreferencesStore",
            "RoomDetailsPresenter: IxPreferencesStore import"
        ))
        add(Expectation(
            "features/roomdetails/impl/src/main/kotlin/io/element/android/features/roomdetails/impl/RoomDetailsView.kt",
            "import de.iltix.lib.R as IltixR",
            "RoomDetailsView: IltixR import"
        ))

        // === User Profile hooks ===
        add(Expectation(
            "features/userprofile/shared/src/main/kotlin/io/element/android/features/userprofile/shared/UserProfileView.kt",
            "import de.iltix.components.nicknames.IxLocalNicknameAction",
            "UserProfileView: IxLocalNicknameAction import"
        ))

        // === Push/Notification hooks ===
        add(Expectation(
            "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/notifications/factories/NotificationCreator.kt",
            "import de.iltix.push.resolveIxNotificationRoute",
            "NotificationCreator: resolveIxNotificationRoute import"
        ))
        add(Expectation(
            "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/notifications/channels/NotificationChannels.kt",
            "import de.iltix.push.createIxPriorityNotificationChannels",
            "NotificationChannels: createIxPriorityNotificationChannels import"
        ))
        add(Expectation(
            "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/workmanager/FetchPendingNotificationsWorker.kt",
            "import de.iltix.push.IxMediaAutoDownloadService",
            "FetchPendingNotificationsWorker: IxMediaAutoDownloadService import"
        ))

        // === Matrix UI hooks ===
        add(Expectation(
            "libraries/matrixui/src/main/kotlin/io/element/android/libraries/matrix/ui/messages/sender/SenderName.kt",
            "import de.iltix.components.nicknames.rememberIxResolvedDisplayName",
            "SenderName: rememberIxResolvedDisplayName import"
        ))

        // === Build system ===
        add(Expectation(
            "settings.gradle.kts",
            """include(":appicon:iltix")""",
            "settings.gradle.kts: iltix appicon include"
        ))
        add(Expectation(
            "settings.gradle.kts",
            """includeProjects(File(rootDir, "iltix"), ":iltix")""",
            "settings.gradle.kts: iltix module auto-include"
        ))
        add(Expectation(
            "plugins/src/main/kotlin/Enterprise.kt",
            "isIltixBuild",
            "Enterprise.kt: isIltixBuild flag"
        ))
        add(Expectation(
            "plugins/src/main/kotlin/config/BuildTimeConfig.kt",
            "ILTIX_APPLICATION_ID",
            "BuildTimeConfig.kt: Iltix constants"
        ))
        add(Expectation(
            "plugins/src/main/kotlin/ModulesConfig.kt",
            "isIltixBuild",
            "ModulesConfig.kt: Iltix analytics routing"
        ))
        add(Expectation(
            "app/build.gradle.kts",
            """"ix"""",
            "app/build.gradle.kts: ix flavor"
        ))
        add(Expectation(
            "appconfig/build.gradle.kts",
            """"ix"""",
            "appconfig/build.gradle.kts: ix flavor"
        ))

        // === Overlay modules exist ===
        add(Expectation(
            "iltix/lib/build.gradle.kts",
            "namespace",
            "iltix/lib module exists"
        ))
        add(Expectation(
            "iltix/components/build.gradle.kts",
            "namespace",
            "iltix/components module exists"
        ))
        add(Expectation(
            "iltix/theme/build.gradle.kts",
            "namespace",
            "iltix/theme module exists"
        ))
    }

    fun verify(): List<VerificationResult> {
        return allExpectations().map { expectation ->
            val file = workspace.resolve(expectation.file)
            if (!file.exists()) {
                VerificationResult(expectation, false, "File not found: ${expectation.file}")
            } else {
                val content = file.readText()
                if (content.contains(expectation.expectedContent)) {
                    VerificationResult(expectation, true, "OK")
                } else {
                    VerificationResult(expectation, false, "Expected content not found in ${expectation.file}: ${expectation.expectedContent.take(60)}")
                }
            }
        }
    }

    fun printReport(results: List<VerificationResult>) {
        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }
        println("\n=== Patch Verification Report ===")
        println("Passed: $passed / ${results.size}")
        if (failed > 0) {
            println("FAILED: $failed")
            results.filter { !it.passed }.forEach { result ->
                println("  ✗ ${result.expectation.description}")
                println("    ${result.message}")
            }
        } else {
            println("All verifications passed!")
        }
    }
}
