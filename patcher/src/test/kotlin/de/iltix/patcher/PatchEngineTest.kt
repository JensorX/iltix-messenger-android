package de.iltix.patcher

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PatchEngineTest {

    @Test
    fun `replaceText tolerates indentation-only changes`() = withWorkspace(
        """fun content() {
    ExpandableBottomSheetLayout(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    )
}
"""
    ) { file, engine ->
        engine.replaceText(
            TARGET_FILE,
            """        ExpandableBottomSheetLayout(
                modifier = modifier
                        .fillMaxSize()
                        .imePadding()""",
            """    Replacement()""",
        )

        assertTrue(engine.failedResults().isEmpty())
        assertTrue(file.readText().contains("    Replacement()"))
        assertFalse(file.readText().contains("ExpandableBottomSheetLayout"))
    }

    @Test
    fun `replaceText tolerates whitespace-only formatting changes`() = withWorkspace(
        """fun content() {
    Target(value = true,
        label = "same value",
    )
}
"""
    ) { file, engine ->
        engine.replaceText(
            TARGET_FILE,
            """    Target(
        value = true,
        label = "same value",
    )""",
            """    Replacement()""",
        )

        assertTrue(engine.failedResults().isEmpty())
        assertTrue(file.readText().contains("    Replacement()"))
        assertFalse(file.readText().contains("Target("))
    }

    @Test
    fun `replaceText handles GeneralSection after ListItem content migration`() = withWorkspace(
        """private fun ColumnScope.GeneralSection(
    onOpenDeveloperSettings: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    ListItem(
        content = { Text(stringResource(id = CommonStrings.common_advanced_settings)) },
                leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Settings())),
                onClick = onOpenAdvancedSettings,
    )
}
"""
    ) { file, engine ->
        engine.replaceText(
            TARGET_FILE,
            """    onOpenDeveloperSettings: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    ListItem(""",
            """    onOpenDeveloperSettings: () -> Unit,
    onOpenIltixModules: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    ListItem(""",
        )

        engine.insertAfterLine(
            TARGET_FILE,
            """onClick = onOpenAdvancedSettings,""",
            """    )

    ListItem(
        content = { Text(stringResource(id = IltixR.string.iltix_modules_title)) },
        leadingContent = ListItemContent.Icon(IconSource.Resource(IltixR.drawable.ic_iltix)),
        onClick = onOpenIltixModules,""",
        )

        assertTrue(engine.failedResults().isEmpty())
        val patchedContent = file.readText()
        assertTrue(patchedContent.contains("onOpenIltixModules: () -> Unit"))
        assertTrue(patchedContent.lines().any {
            it.trim() == "content = { Text(stringResource(id = CommonStrings.common_advanced_settings)) },"
        })
        assertTrue(patchedContent.lines().any { it.trimStart().startsWith("leadingContent =") })
        assertEquals(
            1,
            patchedContent.lines().count {
                it.trim() == "content = { Text(stringResource(id = CommonStrings.common_advanced_settings)) },"
            },
        )
        assertEquals(2, patchedContent.lines().count { it.trimStart().startsWith("content =") })
    }

    @Test
    fun `HomeView patches match v26_10 scaffold structure`() = withWorkspace(
        """package io.element.android.features.home.impl

import androidx.compose.foundation.layout.PaddingValues
import io.element.android.features.home.impl.components.HomeTopBar

private fun HomeScaffold() {
    val state = homeState
    val roomListState: RoomListState = state.roomListState
    val roomsLazyListState = rememberLazyListState()
    val spacesLazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            HomeTopBar(
                selectedNavigationItem = state.currentHomeNavigationBarItem,
                canReportBug = state.canReportBug,
            )
        },
        floatingActionButton = {
            val coroutineScope = rememberCoroutineScope()
            HomeBottomBar(
                floatingActionButton = {
                    when (state.currentHomeNavigationBarItem) {
                        HomeNavigationBarItem.Chats -> {
                            HomeFloatingActionButton(onStartChatClick, CommonStrings.action_create_room)
                        }
                        HomeNavigationBarItem.Spaces -> {
                            HomeFloatingActionButton(onCreateSpaceClick, CommonStrings.action_create_space)
                        }
                    }
                },
                totalUnreadCount = state.totalUnreadCount,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        content = { padding ->
            val outerPadding = PaddingValues()
            val contentPadding = PaddingValues(
                bottom = 96.dp,
            )
            when (state.currentHomeNavigationBarItem) {
                HomeNavigationBarItem.Chats -> {
                    RoomListContentView(
                        contentState = roomListState.contentState,
                        filtersState = roomListState.filtersState,
                        spaceFiltersState = roomListState.spaceFiltersState,
                        lazyListState = roomsLazyListState,
                        hideInvitesAvatars = roomListState.hideInvitesAvatars,
                        eventSink = roomListState.eventSink,
                        onSetUpRecoveryClick = onSetUpRecoveryClick,
                        onConfirmRecoveryKeyClick = onConfirmRecoveryKeyClick,
                        onRoomClick = ::onRoomClick,
                        onCreateRoomClick = onStartChatClick,
                        contentPadding = lazyColumnContentPadding + contentPadding,
                        modifier = Modifier
                            .padding(outerPadding)
                            .consumeWindowInsets(outerPadding)
                    )
                    SpaceFiltersView(roomListState.spaceFiltersState)
                }
            }
        },
    )
}
""",
        HOME_VIEW_FILE,
    ) { file, engine ->
        SourcePatches(engine).applyAll()

        val homeViewFailures = engine.failedResults().filter { it.file == HOME_VIEW_FILE }
        assertTrue(homeViewFailures.isEmpty(), homeViewFailures.joinToString("\n"))

        val patchedContent = file.readText()
        assertTrue(patchedContent.contains("useIltixTheme = ixHomeUi.useIltixTheme"))
        assertTrue(patchedContent.contains("showStartChatInTopBar = ixHomeUi.showStartChatInTopBar"))
        assertTrue(patchedContent.contains("IxHomeChatsContent("))
        assertTrue(patchedContent.contains("totalUnreadCount = state.totalUnreadCount,"))
        assertFalse(patchedContent.contains("RoomListContentView("))
        assertFalse(patchedContent.contains("hazeState = hazeState"))
    }

    @Test
    fun `replaceText rejects semantic changes`() = withWorkspace(
        """fun content() {
    ExpandableBottomSheetLayout(
        modifier = modifier
            .fillMaxWidth()
    )
}
"""
    ) { file, engine ->
        val originalContent = file.readText()

        engine.replaceText(
            TARGET_FILE,
            """    ExpandableBottomSheetLayout(
        modifier = modifier
            .fillMaxSize()""",
            """    Replacement()""",
        )

        assertEquals(originalContent, file.readText())
        assertEquals("Pattern not found", engine.failedResults().single().message)
    }

    @Test
    fun `replaceText rejects ambiguous indentation fallback`() = withWorkspace(
        """fun first() {
    Target(
        value = true,
    )
}

fun second() {
        Target(
            value = true,
        )
}
"""
    ) { file, engine ->
        val originalContent = file.readText()

        engine.replaceText(
            TARGET_FILE,
            """Target(
value = true,""",
            """Replacement()""",
        )

        assertEquals(originalContent, file.readText())
        assertEquals(
            "Indentation-insensitive pattern is ambiguous (2 matches)",
            engine.failedResults().single().message,
        )
    }

    private fun withWorkspace(
        content: String,
        targetFile: String = TARGET_FILE,
        test: (File, PatchEngine) -> Unit,
    ) {
        val workspace = createTempDirectory("iltix-patch-engine-test").toFile()
        try {
            val file = workspace.resolve(targetFile)
            file.parentFile.mkdirs()
            file.writeText(content)
            test(file, PatchEngine(workspace))
        } finally {
            workspace.deleteRecursively()
        }
    }

    private companion object {
        const val TARGET_FILE = "Target.kt"
        const val HOME_VIEW_FILE = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/HomeView.kt"
    }
}
