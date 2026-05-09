/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

plugins {
    id("io.element.android-compose-library")
}

android {
    namespace = "de.iltix.appicon"

    buildTypes {
        register("nightly")
    }
}
