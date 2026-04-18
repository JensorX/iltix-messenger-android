/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.compound.tokens

import androidx.compose.ui.text.TextStyle
import io.element.android.compound.tokens.generated.TypographyTokens

data class CompoundTypographyTokens(
    val fontBodyLgMedium: TextStyle,
    val fontBodyLgRegular: TextStyle,
    val fontBodyMdMedium: TextStyle,
    val fontBodyMdRegular: TextStyle,
    val fontBodySmMedium: TextStyle,
    val fontBodySmRegular: TextStyle,
    val fontBodyXsMedium: TextStyle,
    val fontBodyXsRegular: TextStyle,
    val fontHeadingLgBold: TextStyle,
    val fontHeadingLgRegular: TextStyle,
    val fontHeadingMdBold: TextStyle,
    val fontHeadingMdRegular: TextStyle,
    val fontHeadingSmMedium: TextStyle,
    val fontHeadingSmRegular: TextStyle,
    val fontHeadingXlBold: TextStyle,
    val fontHeadingXlRegular: TextStyle,
)

val defaultCompoundTypographyTokens = CompoundTypographyTokens(
    fontBodyLgMedium = TypographyTokens.fontBodyLgMedium,
    fontBodyLgRegular = TypographyTokens.fontBodyLgRegular,
    fontBodyMdMedium = TypographyTokens.fontBodyMdMedium,
    fontBodyMdRegular = TypographyTokens.fontBodyMdRegular,
    fontBodySmMedium = TypographyTokens.fontBodySmMedium,
    fontBodySmRegular = TypographyTokens.fontBodySmRegular,
    fontBodyXsMedium = TypographyTokens.fontBodyXsMedium,
    fontBodyXsRegular = TypographyTokens.fontBodyXsRegular,
    fontHeadingLgBold = TypographyTokens.fontHeadingLgBold,
    fontHeadingLgRegular = TypographyTokens.fontHeadingLgRegular,
    fontHeadingMdBold = TypographyTokens.fontHeadingMdBold,
    fontHeadingMdRegular = TypographyTokens.fontHeadingMdRegular,
    fontHeadingSmMedium = TypographyTokens.fontHeadingSmMedium,
    fontHeadingSmRegular = TypographyTokens.fontHeadingSmRegular,
    fontHeadingXlBold = TypographyTokens.fontHeadingXlBold,
    fontHeadingXlRegular = TypographyTokens.fontHeadingXlRegular,
)
