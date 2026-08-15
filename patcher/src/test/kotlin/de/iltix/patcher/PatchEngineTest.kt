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
    fun `replaceText handles GeneralSection after ListItem content migration`() = withWorkspace(
        """private fun ColumnScope.GeneralSection(
    onOpenDeveloperSettings: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    ListItem(
        content = { Text(stringResource(id = CommonStrings.common_advanced_settings)) },
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

        assertTrue(engine.failedResults().isEmpty())
        assertTrue(file.readText().contains("onOpenIltixModules: () -> Unit"))
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
        test: (File, PatchEngine) -> Unit,
    ) {
        val workspace = createTempDirectory("iltix-patch-engine-test").toFile()
        try {
            val file = workspace.resolve(TARGET_FILE)
            file.writeText(content)
            test(file, PatchEngine(workspace))
        } finally {
            workspace.deleteRecursively()
        }
    }

    private companion object {
        const val TARGET_FILE = "Target.kt"
    }
}
