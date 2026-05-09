package de.iltix.patcher

import java.io.File

/**
 * Applies all build system patches (Gradle files, plugin files, settings).
 * These configure the ix flavor, add Iltix module dependencies, and set up
 * build-time constants.
 */
class BuildSystemPatches(private val engine: PatchEngine) {

    fun applyAll() {
        println("\n--- Applying build system patches ---")
        patchVersionCatalog()
        patchSettingsGradle()
        patchEnterprise()
        patchBuildTimeConfig()
        patchModulesConfig()
        patchAppBuildGradle()
        patchAppconfigBuildGradle()
        patchAppnavBuildGradle()
        patchHomeImplBuildGradle()
        patchMessagesImplBuildGradle()
        patchPreferencesImplBuildGradle()
        patchRoomDetailsImplBuildGradle()
        patchUserProfileSharedBuildGradle()
        patchPushImplBuildGradle()
        patchMatrixUiBuildGradle()

        patchKoverExtension()
        patchCommonExtension()

        val results = engine.getResults()
        val failures = engine.failedResults()
        println("  Build system patches: ${results.size - failures.size} succeeded, ${failures.size} failed")
        if (failures.isNotEmpty()) {
            failures.forEach { println("    ✗ ${it.file}: ${it.operation} — ${it.message}") }
        }
    }

    // ===== Version catalog =====

    private fun patchVersionCatalog() {
        val path = "gradle/libs.versions.toml"

        // Add Room version after an existing version entry
        engine.insertAfterLine(
            path,
            """^ksp = """,
            """room = "2.6.1"""",
            "libs.versions.toml: room version"
        )

        // Add Room libraries after existing androidx entries
        engine.insertAfterLine(
            path,
            """androidx_datastore_datastore""",
            """androidx_room_runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx_room_ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx_room_compiler = { module = "androidx.room:room-compiler", version.ref = "room" }""",
            "libs.versions.toml: room libraries"
        )

        // Add coroutines.android after coroutines_core
        engine.insertAfterLine(
            path,
            """coroutines_core = """,
            """coroutines_android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }""",
            "libs.versions.toml: coroutines android"
        )
    }

    // ===== settings.gradle.kts =====

    private fun patchSettingsGradle() {
        val path = "settings.gradle.kts"

        // Add iltix appicon include after enterprise appicon
        engine.insertAfterLine(
            path,
            """include\(":appicon:enterprise"\)""",
            """include(":appicon:iltix")""",
            "settings: appicon:iltix include"
        )

        // Add iltix module auto-discovery at end of file
        engine.insertAfterLine(
            path,
            """includeProjects\(File\(rootDir, "services"\)""",
            """includeProjects(File(rootDir, "iltix"), ":iltix")""",
            "settings: iltix auto-include"
        )
    }

    // ===== plugins/ =====

    private fun patchEnterprise() {
        val path = "plugins/src/main/kotlin/Enterprise.kt"

        // Add isIltixBuild flag after isEnterpriseBuild
        engine.insertAfterLine(
            path,
            """val isEnterpriseBuild = File\("enterprise/README.md"\)\.exists\(\)""",
            """
/**
 * Are we building the Iltix variant?
 */
val isIltixBuild = File("iltix/lib/build.gradle.kts").exists()""",
            "Enterprise.kt: isIltixBuild flag"
        )
    }

    private fun patchBuildTimeConfig() {
        val path = "plugins/src/main/kotlin/config/BuildTimeConfig.kt"

        // Add Iltix constants after Element constants
        engine.insertAfterLine(
            path,
            """const val GOOGLE_APP_ID_NIGHTLY""",
            """
    // Iltix app values
    const val ILTIX_APPLICATION_ID = "de.iltix.messenger"
    const val ILTIX_APPLICATION_NAME = "Iltix"
    const val ILTIX_GOOGLE_APP_ID_RELEASE = "1:912726360885:android:d097de99a4c23d2700427c"
    const val ILTIX_GOOGLE_APP_ID_DEBUG = "1:912726360885:android:def0a4e454042e9b00427c"
    const val ILTIX_GOOGLE_APP_ID_NIGHTLY = "1:912726360885:android:e17435e0beb0303000427c"""",
            "BuildTimeConfig: Iltix constants"
        )
    }

    private fun patchModulesConfig() {
        val path = "plugins/src/main/kotlin/ModulesConfig.kt"

        // Insert Iltix analytics check before the enterprise check
        // Upstream: `val analyticsConfig: AnalyticsConfig = if (isEnterpriseBuild) {`
        // Target:   `val analyticsConfig: AnalyticsConfig = if (isIltixBuild) { ... } else if (isEnterpriseBuild) {`
        engine.replaceText(
            path,
            "val analyticsConfig: AnalyticsConfig = if (isEnterpriseBuild) {",
            """val analyticsConfig: AnalyticsConfig = if (isIltixBuild) {
        println("Analytics disabled (Iltix build)")
        AnalyticsConfig.Disabled
    } else if (isEnterpriseBuild) {"""
        )
    }

    // ===== app/build.gradle.kts =====

    private fun patchAppBuildGradle() {
        val path = "app/build.gradle.kts"

        // Add ResValue import (needed for ix variant app_name override)
        engine.addImport(path, "com.android.build.api.variant.ResValue")

        // Add "app" flavor dimension after "store" dimension
        engine.insertAfterLine(
            path,
            """flavorDimensions \+= "store"""",
            """    flavorDimensions += "app"""",
            "app/build.gradle.kts: app flavor dimension"
        )

        // Add element + ix flavors after fdroid flavor block
        engine.insertAfterBlock(
            path,
            """create\("fdroid"\)\s*\{[^}]*\}""",
            """        create("element") {
            dimension = "app"
            isDefault = true
            buildConfigFieldStr("APP_VARIANT", "Element")
        }
        create("ix") {
            dimension = "app"
            applicationId = BuildTimeConfig.ILTIX_APPLICATION_ID
            buildConfigFieldStr("APP_VARIANT", "Iltix")
        }""",
            "app/build.gradle.kts: element + ix flavors"
        )

        // Add ix variant app_name override in onVariants block
        engine.insertAfterBlock(
            path,
            """output\.versionCode\.set\(\(output\.versionCode\.orNull \?: 0\) \* 10 \+ abiCode\)\s*\}""",
            """        if (variant.productFlavors.any { (_, flavor) -> flavor == "ix" }) {
            val iltixBaseAppName = BuildTimeConfig.ILTIX_APPLICATION_NAME
            val iltixAppName = when (variant.buildType) {
                "debug" -> "${'$'}iltixBaseAppName dbg"
                "nightly" -> "${'$'}iltixBaseAppName nightly"
                else -> iltixBaseAppName
            }
            variant.resValues.put(
                variant.makeResValueKey("string", "app_name"),
                ResValue(iltixAppName),
            )
        }""",
            "app/build.gradle.kts: ix variant app_name"
        )

        // Add Iltix dependencies in the else branch (non-enterprise)
        // Replace the unconditional appicon.element with flavor-specific deps
        engine.replaceText(
            path,
            """implementation(projects.features.enterprise.implFoss)
        implementation(projects.appicon.element)""",
            """implementation(projects.features.enterprise.implFoss)
        implementation(projects.iltix.theme)
        "elementImplementation"(projects.appicon.element)
        "ixImplementation"(projects.appicon.iltix)
        "ixImplementation"(projects.iltix.lib)
        "ixImplementation"(projects.iltix.components)"""
        )
    }

    // ===== appconfig/build.gradle.kts =====

    private fun patchAppconfigBuildGradle() {
        val path = "appconfig/build.gradle.kts"

        // Upstream uses defaultConfig {} for URL_POLICY etc.
        // We need to replace it with productFlavors { element { ... } ix { ... } }
        // Strategy: Insert flavorDimensions + productFlavors block after buildFeatures,
        // and wrap the existing defaultConfig content into the element flavor.
        engine.replaceText(
            path,
            """    defaultConfig {
        buildConfigFieldStr(
            name = "URL_POLICY",
            value = if (isEnterpriseBuild) {
                BuildTimeConfig.URL_POLICY ?: ""
            } else {
                "https://element.io/cookie-policy"
            },
        )
        buildConfigFieldStr(
            name = "BUG_REPORT_URL",
            value = if (isEnterpriseBuild) {
                BuildTimeConfig.BUG_REPORT_URL ?: ""
            } else {
                "https://rageshakes.element.io/api/submit"
            },
        )
        buildConfigFieldStr(
            name = "BUG_REPORT_APP_NAME",
            value = if (isEnterpriseBuild) {
                BuildTimeConfig.BUG_REPORT_APP_NAME ?: ""
            } else {
                "element-x-android"
            },
        )
    }""",
            """    flavorDimensions += "app"

    productFlavors {
        create("element") {
            dimension = "app"
            isDefault = true
            buildConfigFieldStr(
                name = "URL_POLICY",
                value = if (isEnterpriseBuild) {
                    BuildTimeConfig.URL_POLICY ?: ""
                } else {
                    "https://element.io/cookie-policy"
                },
            )
            buildConfigFieldStr(
                name = "BUG_REPORT_URL",
                value = if (isEnterpriseBuild) {
                    BuildTimeConfig.BUG_REPORT_URL ?: ""
                } else {
                    "https://rageshakes.element.io/api/submit"
                },
            )
            buildConfigFieldStr(
                name = "BUG_REPORT_APP_NAME",
                value = if (isEnterpriseBuild) {
                    BuildTimeConfig.BUG_REPORT_APP_NAME ?: ""
                } else {
                    "element-x-android"
                },
            )
        }
        create("ix") {
            dimension = "app"
            buildConfigFieldStr(
                name = "URL_POLICY",
                value = "https://iltix.messenger",
            )
            buildConfigFieldStr(
                name = "BUG_REPORT_URL",
                value = "https://github.com/iltix/iltix-messenger-android/issues",
            )
            buildConfigFieldStr(
                name = "BUG_REPORT_APP_NAME",
                value = "iltix-messenger-android",
            )
        }
    }"""
        )
    }

    // ===== Feature/Library build.gradle.kts dependencies =====

    private fun patchAppnavBuildGradle() {
        engine.addGradleDependency(
            "appnav/build.gradle.kts",
            "implementation(projects.iltix.theme)"
        )
    }

    private fun patchHomeImplBuildGradle() {
        val path = "features/home/impl/build.gradle.kts"
        engine.addGradleDependency(path, "implementation(projects.iltix.components)")
        engine.addGradleDependency(path, "implementation(projects.iltix.lib)")
    }

    private fun patchMessagesImplBuildGradle() {
        val path = "features/messages/impl/build.gradle.kts"
        engine.addGradleDependency(path, "implementation(projects.iltix.components)")
        engine.addGradleDependency(path, "implementation(projects.iltix.lib)")
        engine.addGradleDependency(path, "implementation(projects.iltix.theme)")
    }

    private fun patchPreferencesImplBuildGradle() {
        engine.addGradleDependency(
            "features/preferences/impl/build.gradle.kts",
            "implementation(projects.iltix.lib)"
        )
        engine.addGradleDependency(
            "features/preferences/impl/build.gradle.kts",
            "implementation(projects.iltix.components)"
        )
    }

    private fun patchRoomDetailsImplBuildGradle() {
        engine.addGradleDependency(
            "features/roomdetails/impl/build.gradle.kts",
            "implementation(projects.iltix.lib)"
        )
    }

    private fun patchUserProfileSharedBuildGradle() {
        engine.addGradleDependency(
            "features/userprofile/shared/build.gradle.kts",
            "implementation(projects.iltix.components)"
        )
    }

    private fun patchPushImplBuildGradle() {
        engine.addGradleDependency(
            "libraries/push/impl/build.gradle.kts",
            "implementation(projects.iltix.lib)"
        )
    }

    private fun patchMatrixUiBuildGradle() {
        engine.addGradleDependency(
            "libraries/matrixui/build.gradle.kts",
            "implementation(projects.iltix.components)"
        )
    }

    // ===== Kover variant fix =====

    private fun patchKoverExtension() {
        // Adding the "app" flavor dimension changes variant names:
        // "gplayDebug" → "gplayElementDebug" (or "gplayIxDebug")
        // KoverExtension references the old name — update it.
        engine.replaceText(
            "plugins/src/main/kotlin/extension/KoverExtension.kt",
            """addWithDependencies("gplayDebug")""",
            """addWithDependencies("gplayElementDebug")"""
        )
    }

    // ===== CommonExtension — missingDimensionStrategy for "app" dimension =====

    private fun patchCommonExtension() {
        val path = "plugins/src/main/kotlin/extension/CommonExtension.kt"
        engine.replaceText(
            path,
            "generatedDensities()\n        }\n    }",
            "generatedDensities()\n        }\n        missingDimensionStrategy(\"app\", \"element\")\n        missingDimensionStrategy(\"store\", \"gplay\")\n    }"
        )
    }
}
