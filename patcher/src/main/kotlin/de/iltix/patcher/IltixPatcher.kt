package de.iltix.patcher

import java.io.File
import java.util.Properties

/**
 * Main entry point for the Iltix patcher.
 *
 * Usage:
 *   java -jar patcher.jar [--root <project-root>] [--upstream-tag <tag>] [--skip-fetch]
 *
 * Steps:
 *   1. Read iltix.upstream.properties for the upstream tag
 *   2. Fetch upstream Element X at that tag (unless --skip-fetch)
 *   3. Copy upstream into workspace/
 *   4. Apply overlay/ files (additive Iltix modules)
 *   5. Apply source patches (Kotlin hooks)
 *   6. Apply build system patches (Gradle/plugin modifications)
 *   7. Verify all patches applied correctly
 */
fun main(args: Array<String>) {
    val config = parseArgs(args)
    val patcher = IltixPatcher(config)
    val success = patcher.run()
    if (!success) {
        System.exit(1)
    }
}

data class PatcherConfig(
    val projectRoot: File,
    val upstreamTag: String?,
    val skipFetch: Boolean,
)

fun parseArgs(args: Array<String>): PatcherConfig {
    var root = File(System.getProperty("user.dir")).parentFile // patcher/ is a subdir
    var upstreamTag: String? = null
    var skipFetch = false

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--root" -> { root = File(args[++i]); i++ }
            "--upstream-tag" -> { upstreamTag = args[++i]; i++ }
            "--skip-fetch" -> { skipFetch = true; i++ }
            else -> { i++ }
        }
    }

    return PatcherConfig(root, upstreamTag, skipFetch)
}

class IltixPatcher(private val config: PatcherConfig) {

    private val projectRoot = config.projectRoot
    private val workspace = projectRoot.resolve("workspace")
    private val overlayDir = projectRoot.resolve("overlay")
    private val upstreamCache = projectRoot.resolve(".upstream-cache")
    private val propertiesFile = projectRoot.resolve("iltix.upstream.properties")

    fun run(): Boolean {
        println("=== Iltix Patcher ===")
        println("Project root: ${projectRoot.absolutePath}")
        println("Workspace:    ${workspace.absolutePath}")

        // 1. Read config
        val props = readProperties()
        val tag = config.upstreamTag ?: props.getProperty("UPSTREAM_TAG")
            ?: error("No upstream tag specified. Set UPSTREAM_TAG in iltix.upstream.properties or use --upstream-tag")
        val repo = props.getProperty("UPSTREAM_REPO", "element-hq/element-x-android")
        println("Upstream: $repo @ $tag")

        // 2. Fetch upstream
        if (!config.skipFetch) {
            fetchUpstream(repo, tag)
        } else {
            println("\n[SKIP] Fetching upstream (--skip-fetch)")
            if (!upstreamCache.resolve(tag).exists()) {
                error("Upstream cache for tag $tag not found. Run without --skip-fetch first.")
            }
        }

        // 3. Prepare workspace
        prepareWorkspace(tag)

        // 4. Apply overlay
        println("\n--- Applying overlay ---")
        val overlayApplier = OverlayApplier(overlayDir, workspace)
        overlayApplier.apply()

        // 5. Apply source patches
        val sourceEngine = PatchEngine(workspace)
        val sourcePatches = SourcePatches(sourceEngine)
        sourcePatches.applyAll()

        // 6. Apply build system patches
        val buildEngine = PatchEngine(workspace)
        val buildPatches = BuildSystemPatches(buildEngine)
        buildPatches.applyAll()

        // 7. Verify
        println("\n--- Verifying patches ---")
        val verifier = PatchVerifier(workspace)
        val verificationResults = verifier.verify()
        verifier.printReport(verificationResults)

        val allPassed = verificationResults.all { it.passed }
        val sourceOk = !sourceEngine.hasFailures()
        val buildOk = !buildEngine.hasFailures()

        if (allPassed && sourceOk && buildOk) {
            println("\n✓ Workspace ready at: ${workspace.absolutePath}")
            println("  Open this directory in Android Studio to develop.")
            return true
        } else {
            println("\n✗ Patching completed with errors. Check the reports above.")
            return false
        }
    }

    private fun readProperties(): Properties {
        val props = Properties()
        if (propertiesFile.exists()) {
            propertiesFile.inputStream().use { props.load(it) }
        }
        return props
    }

    private fun fetchUpstream(repo: String, tag: String) {
        val tagCacheDir = upstreamCache.resolve(tag)
        if (tagCacheDir.exists() && tagCacheDir.list()?.isNotEmpty() == true) {
            println("\n[CACHE] Upstream $tag already cached at ${tagCacheDir.absolutePath}")
            return
        }

        println("\n--- Fetching upstream $repo @ $tag ---")
        tagCacheDir.mkdirs()

        val url = "https://github.com/$repo/archive/refs/tags/$tag.tar.gz"
        val tarFile = upstreamCache.resolve("$tag.tar.gz")

        // Download
        println("  Downloading $url ...")
        val downloadResult = runCommand(
            "curl", "-L", "-o", tarFile.absolutePath, url,
            workDir = upstreamCache
        )
        if (downloadResult != 0) {
            error("Failed to download upstream archive (exit code $downloadResult)")
        }

        // Extract
        println("  Extracting...")
        val extractResult = runCommand(
            "tar", "xzf", tarFile.absolutePath,
            "--strip-components=1",
            "-C", tagCacheDir.absolutePath,
            workDir = upstreamCache
        )
        if (extractResult != 0) {
            error("Failed to extract upstream archive (exit code $extractResult)")
        }

        // Clean up tar
        tarFile.delete()
        println("  Cached at ${tagCacheDir.absolutePath}")
    }

    private fun prepareWorkspace(tag: String) {
        val tagCacheDir = upstreamCache.resolve(tag)
        println("\n--- Preparing workspace ---")

        if (workspace.exists()) {
            println("  Cleaning existing workspace...")
            workspace.deleteRecursively()
        }

        println("  Copying upstream $tag to workspace...")
        tagCacheDir.copyRecursively(workspace)
        println("  Workspace prepared: ${workspace.listFiles()?.size ?: 0} top-level entries")
    }

    private fun runCommand(vararg cmd: String, workDir: File = projectRoot): Int {
        val process = ProcessBuilder(*cmd)
            .directory(workDir)
            .inheritIO()
            .start()
        return process.waitFor()
    }
}
