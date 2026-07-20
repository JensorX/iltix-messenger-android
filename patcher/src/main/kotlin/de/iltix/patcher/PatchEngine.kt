package de.iltix.patcher

import java.io.File

/**
 * Core engine for applying pattern-based source code patches.
 * Uses regex anchors instead of line numbers for robustness against upstream changes.
 */
class PatchEngine(private val workspace: File) {

    data class PatchResult(
        val file: String,
        val operation: String,
        val success: Boolean,
        val message: String,
    )

    private val results = mutableListOf<PatchResult>()

    fun getResults(): List<PatchResult> = results.toList()
    fun hasFailures(): Boolean = results.any { !it.success }

    fun failedResults(): List<PatchResult> = results.filter { !it.success }

    /**
     * Add an import statement after the last existing import in the file.
     * Idempotent: skips if the import already exists.
     */
    fun addImport(relativePath: String, import: String) {
        val file = workspace.resolve(relativePath)
        if (!file.exists()) {
            results.add(PatchResult(relativePath, "addImport($import)", false, "File not found"))
            return
        }
        val content = file.readText()
        val importStatement = "import $import"

        // Use whole-line match to avoid substring false positives
        // (e.g. "remember" matching "rememberCoroutineScope")
        if (content.lines().any { it.trim() == importStatement }) {
            results.add(PatchResult(relativePath, "addImport($import)", true, "Already present"))
            return
        }

        // Find the last import statement and insert after it
        val lastImportRegex = Regex("^import .+$", RegexOption.MULTILINE)
        val matches = lastImportRegex.findAll(content).toList()
        if (matches.isEmpty()) {
            results.add(PatchResult(relativePath, "addImport($import)", false, "No existing imports found"))
            return
        }

        val lastImport = matches.last()
        val insertPos = lastImport.range.last + 1
        val newContent = content.substring(0, insertPos) + "\n" + importStatement + content.substring(insertPos)
        file.writeText(newContent)
        results.add(PatchResult(relativePath, "addImport($import)", true, "Inserted after last import"))
    }

    /**
     * Replace all occurrences of a literal string in a file.
     * Multi-line replacements retry with indentation-insensitive whole-line matching.
     * This tolerates formatter-only indentation changes while preserving line content and structure.
     */
    fun replaceText(relativePath: String, oldText: String, newText: String) {
        val file = workspace.resolve(relativePath)
        if (!file.exists()) {
            results.add(PatchResult(relativePath, "replaceText(${oldText.take(40)}...)", false, "File not found"))
            return
        }
        val content = file.readText()
        if (content.contains(oldText)) {
            val newContent = content.replace(oldText, newText)
            file.writeText(newContent)
            val count = Regex(Regex.escape(oldText)).findAll(content).count()
            results.add(PatchResult(relativePath, "replaceText(${oldText.take(40)}...)", true, "Replaced $count occurrence(s)"))
            return
        }

        val matches = findIndentationInsensitiveMatches(content, oldText)
        when (matches.size) {
            1 -> {
                file.writeText(content.replaceRange(matches.single().range, newText))
                results.add(PatchResult(
                    relativePath,
                    "replaceText(${oldText.take(40)}...)",
                    true,
                    "Replaced 1 occurrence using indentation-insensitive fallback",
                ))
            }
            0 -> results.add(PatchResult(relativePath, "replaceText(${oldText.take(40)}...)", false, "Pattern not found"))
            else -> results.add(PatchResult(
                relativePath,
                "replaceText(${oldText.take(40)}...)",
                false,
                "Indentation-insensitive pattern is ambiguous (${matches.size} matches)",
            ))
        }
    }

    /**
     * Replace occurrences of a literal string when present.
     * If the pattern is absent, this is treated as a successful no-op.
     */
    fun replaceTextIfPresent(relativePath: String, oldText: String, newText: String, description: String = "") {
        val file = workspace.resolve(relativePath)
        if (!file.exists()) {
            results.add(PatchResult(relativePath, "replaceTextIfPresent($description)", false, "File not found"))
            return
        }
        val content = file.readText()
        if (content.contains(oldText)) {
            val newContent = content.replace(oldText, newText)
            file.writeText(newContent)
            val count = Regex(Regex.escape(oldText)).findAll(content).count()
            results.add(PatchResult(relativePath, "replaceTextIfPresent($description)", true, "Replaced $count occurrence(s)"))
            return
        }

        val matches = findIndentationInsensitiveMatches(content, oldText)
        when (matches.size) {
            1 -> {
                file.writeText(content.replaceRange(matches.single().range, newText))
                results.add(PatchResult(
                    relativePath,
                    "replaceTextIfPresent($description)",
                    true,
                    "Replaced 1 occurrence using indentation-insensitive fallback",
                ))
            }
            0 -> results.add(PatchResult(relativePath, "replaceTextIfPresent($description)", true, "Pattern not found, skipped"))
            else -> results.add(PatchResult(
                relativePath,
                "replaceTextIfPresent($description)",
                false,
                "Indentation-insensitive pattern is ambiguous (${matches.size} matches)",
            ))
        }
    }

    private fun findIndentationInsensitiveMatches(content: String, text: String): List<MatchResult> {
        val normalizedText = text.replace("\r\n", "\n").replace('\r', '\n')
        if (!normalizedText.contains('\n')) return emptyList()

        val pattern = normalizedText.lines().joinToString("""\R""") { line ->
            if (line.isBlank()) {
                """^\h*$"""
            } else {
                """^\h*${Regex.escape(line.trim())}\h*$"""
            }
        }
        return Regex(pattern, RegexOption.MULTILINE).findAll(content).toList()
    }

    /**
     * Replace text matching a regex pattern.
     */
    fun replacePattern(relativePath: String, pattern: String, replacement: String, description: String = "") {
        val file = workspace.resolve(relativePath)
        if (!file.exists()) {
            results.add(PatchResult(relativePath, "replacePattern($description)", false, "File not found"))
            return
        }
        val content = file.readText()
        val regex = Regex(pattern, RegexOption.MULTILINE)
        if (!regex.containsMatchIn(content)) {
            results.add(PatchResult(relativePath, "replacePattern($description)", false, "Pattern not found: $pattern"))
            return
        }
        val newContent = regex.replace(content, replacement)
        file.writeText(newContent)
        results.add(PatchResult(relativePath, "replacePattern($description)", true, "Applied"))
    }

    /**
     * Insert code block after a line matching the given pattern.
     * The anchor pattern matches against individual lines.
     */
    fun insertAfterLine(relativePath: String, anchorPattern: String, codeToInsert: String, description: String = "") {
        val file = workspace.resolve(relativePath)
        if (!file.exists()) {
            results.add(PatchResult(relativePath, "insertAfterLine($description)", false, "File not found"))
            return
        }
        val content = file.readText()
        val anchorRegex = Regex(anchorPattern)
        val lines = content.lines().toMutableList()
        var inserted = false

        for (i in lines.indices) {
            if (anchorRegex.containsMatchIn(lines[i])) {
                lines.add(i + 1, codeToInsert)
                inserted = true
                break
            }
        }

        if (!inserted) {
            results.add(PatchResult(relativePath, "insertAfterLine($description)", false, "Anchor not found: $anchorPattern"))
            return
        }
        file.writeText(lines.joinToString("\n"))
        results.add(PatchResult(relativePath, "insertAfterLine($description)", true, "Inserted"))
    }

    /**
     * Insert code block before a line matching the given pattern.
     */
    fun insertBeforeLine(relativePath: String, anchorPattern: String, codeToInsert: String, description: String = "") {
        val file = workspace.resolve(relativePath)
        if (!file.exists()) {
            results.add(PatchResult(relativePath, "insertBeforeLine($description)", false, "File not found"))
            return
        }
        val content = file.readText()
        val anchorRegex = Regex(anchorPattern)
        val lines = content.lines().toMutableList()
        var inserted = false

        for (i in lines.indices) {
            if (anchorRegex.containsMatchIn(lines[i])) {
                lines.add(i, codeToInsert)
                inserted = true
                break
            }
        }

        if (!inserted) {
            results.add(PatchResult(relativePath, "insertBeforeLine($description)", false, "Anchor not found: $anchorPattern"))
            return
        }
        file.writeText(lines.joinToString("\n"))
        results.add(PatchResult(relativePath, "insertBeforeLine($description)", true, "Inserted"))
    }

    /**
     * Insert a multi-line block after a multi-line anchor pattern (regex across the full file content).
     */
    fun insertAfterBlock(relativePath: String, anchorPattern: String, codeToInsert: String, description: String = "") {
        val file = workspace.resolve(relativePath)
        if (!file.exists()) {
            results.add(PatchResult(relativePath, "insertAfterBlock($description)", false, "File not found"))
            return
        }
        val content = file.readText()
        val regex = Regex(anchorPattern, setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
        val match = regex.find(content)
        if (match == null) {
            results.add(PatchResult(relativePath, "insertAfterBlock($description)", false, "Anchor block not found: $anchorPattern"))
            return
        }
        val insertPos = match.range.last + 1
        val newContent = content.substring(0, insertPos) + "\n" + codeToInsert + content.substring(insertPos)
        file.writeText(newContent)
        results.add(PatchResult(relativePath, "insertAfterBlock($description)", true, "Inserted after block"))
    }

    /**
     * Replace an entire file with new content (for overlay-replace strategy).
     */
    fun replaceFile(relativePath: String, newContent: String) {
        val file = workspace.resolve(relativePath)
        file.parentFile.mkdirs()
        file.writeText(newContent)
        results.add(PatchResult(relativePath, "replaceFile", true, "File replaced"))
    }

    /**
     * Verify that a file contains an expected string after patching.
     */
    fun verify(relativePath: String, expectedContent: String, description: String = ""): Boolean {
        val file = workspace.resolve(relativePath)
        if (!file.exists()) {
            results.add(PatchResult(relativePath, "verify($description)", false, "File not found"))
            return false
        }
        val contains = file.readText().contains(expectedContent)
        if (!contains) {
            results.add(PatchResult(relativePath, "verify($description)", false, "Expected content not found: ${expectedContent.take(60)}..."))
        }
        return contains
    }

    /**
     * Append content to the end of a Gradle dependencies block.
     * Finds `dependencies {` and inserts before its closing `}`.
     */
    fun addGradleDependency(relativePath: String, dependency: String) {
        val file = workspace.resolve(relativePath)
        if (!file.exists()) {
            results.add(PatchResult(relativePath, "addGradleDependency($dependency)", false, "File not found"))
            return
        }
        val content = file.readText()

        if (content.contains(dependency)) {
            results.add(PatchResult(relativePath, "addGradleDependency($dependency)", true, "Already present"))
            return
        }

        // Find the dependencies block and its closing brace
        val depsStart = content.indexOf("dependencies {")
        if (depsStart == -1) {
            results.add(PatchResult(relativePath, "addGradleDependency($dependency)", false, "No dependencies block found"))
            return
        }

        // Find matching closing brace by counting braces
        var braceCount = 0
        var closingBracePos = -1
        for (i in depsStart until content.length) {
            when (content[i]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        closingBracePos = i
                        break
                    }
                }
            }
        }

        if (closingBracePos == -1) {
            results.add(PatchResult(relativePath, "addGradleDependency($dependency)", false, "Could not find closing brace of dependencies block"))
            return
        }

        val newContent = content.substring(0, closingBracePos) +
            "    $dependency\n" +
            content.substring(closingBracePos)
        file.writeText(newContent)
        results.add(PatchResult(relativePath, "addGradleDependency($dependency)", true, "Added to dependencies block"))
    }

    /**
     * Insert a Gradle block (e.g. a product flavor) after a line matching the anchor.
     * Used for inserting flavor definitions, source sets, etc.
     */
    fun insertGradleBlock(relativePath: String, anchorPattern: String, blockToInsert: String, description: String = "") {
        insertAfterBlock(relativePath, anchorPattern, blockToInsert, description)
    }

    /**
     * Add a line to settings.gradle.kts (e.g. include statement).
     * Inserts after the last matching include line or at the given anchor.
     */
    fun addSettingsInclude(relativePath: String, includeLine: String) {
        val file = workspace.resolve(relativePath)
        if (!file.exists()) {
            results.add(PatchResult(relativePath, "addSettingsInclude($includeLine)", false, "File not found"))
            return
        }
        val content = file.readText()
        if (content.contains(includeLine)) {
            results.add(PatchResult(relativePath, "addSettingsInclude($includeLine)", true, "Already present"))
            return
        }

        // Find last include(":...") line and insert after it
        val includeRegex = Regex("""^include\(":.*"\)\s*$""", RegexOption.MULTILINE)
        val matches = includeRegex.findAll(content).toList()
        if (matches.isEmpty()) {
            results.add(PatchResult(relativePath, "addSettingsInclude($includeLine)", false, "No existing include() found"))
            return
        }

        val lastInclude = matches.last()
        val insertPos = lastInclude.range.last + 1
        val newContent = content.substring(0, insertPos) + "\n" + includeLine + content.substring(insertPos)
        file.writeText(newContent)
        results.add(PatchResult(relativePath, "addSettingsInclude($includeLine)", true, "Inserted after last include"))
    }
}
