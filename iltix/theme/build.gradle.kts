/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

plugins {
    id("io.element.android-compose-library")
}

android {
    namespace = "de.iltix.theme"
}

dependencies {
    implementation(projects.iltix.lib)
    api(projects.libraries.compound)
    implementation(projects.libraries.core)
    implementation(projects.libraries.designsystem)
    implementation(projects.libraries.preferences.api)
    implementation(projects.libraries.androidutils)

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.androidx.compose.material3)
}
