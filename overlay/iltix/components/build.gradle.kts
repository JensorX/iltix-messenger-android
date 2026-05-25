/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

plugins {
    id("io.element.android-compose-library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "de.iltix.components"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(projects.iltix.lib)
    implementation(projects.libraries.compound)
    implementation(projects.libraries.designsystem)
    implementation(projects.libraries.androidutils)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.uiStrings)
    implementation(projects.features.poll.api)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.haze)
    implementation(libs.haze.materials)
}
