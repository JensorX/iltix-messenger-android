import config.BuildTimeConfig
import extension.buildConfigFieldStr

/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.appconfig"

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "app"

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
    }
}

dependencies {
    implementation(libs.androidx.annotationjvm)
    implementation(projects.libraries.matrix.api)
}
