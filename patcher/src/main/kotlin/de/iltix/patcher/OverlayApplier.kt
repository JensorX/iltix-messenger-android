package de.iltix.patcher

import java.io.File

/**
 * Copies all files from the overlay/ directory into the workspace.
 * Overlay files are purely additive — they either create new files
 * or replace upstream files entirely (e.g. CompoundTypographyAliases.kt).
 */
class OverlayApplier(
    private val overlayDir: File,
    private val workspace: File,
) {
    data class OverlayResult(
        val relativePath: String,
        val action: String, // "created" | "replaced" | "skipped"
    )

    fun apply(): List<OverlayResult> {
        val results = mutableListOf<OverlayResult>()

        if (!overlayDir.exists()) {
            println("  [WARN] Overlay directory does not exist: ${overlayDir.absolutePath}")
            return results
        }

        overlayDir.walkTopDown()
            .filter { it.isFile }
            .forEach { sourceFile ->
                val relativePath = sourceFile.relativeTo(overlayDir).path
                val targetFile = workspace.resolve(relativePath)

                targetFile.parentFile.mkdirs()

                val action = if (targetFile.exists()) "replaced" else "created"
                sourceFile.copyTo(targetFile, overwrite = true)
                results.add(OverlayResult(relativePath, action))
            }

        println("  Overlay applied: ${results.count { it.action == "created" }} created, ${results.count { it.action == "replaced" }} replaced")
        return results
    }
}
