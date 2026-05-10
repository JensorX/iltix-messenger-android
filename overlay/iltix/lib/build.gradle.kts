/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

plugins {
    id("io.element.android-library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "de.iltix.lib"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.corektx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.opusencoder)

    ksp(libs.androidx.room.compiler)
}
