/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import de.iltix.lib.R
import io.element.android.compound.tokens.generated.TypographyTokens

private val ixNotoSansFontFamily = FontFamily(
    Font(resId = R.font.noto_sans_regular, weight = FontWeight.W400),
    Font(resId = R.font.noto_sans_medium, weight = FontWeight.W500),
    Font(resId = R.font.noto_sans_bold, weight = FontWeight.W700),
    Font(resId = R.font.noto_sans_italic, weight = FontWeight.W400, style = FontStyle.Italic),
    Font(resId = R.font.noto_sans_medium_italic, weight = FontWeight.W500, style = FontStyle.Italic),
    Font(resId = R.font.noto_sans_bold_italic, weight = FontWeight.W700, style = FontStyle.Italic),
)

/**
 * Builds a Material [Typography] using Iltix Noto Sans font,
 * mapping Compound tokens to M3 slots the same way upstream does.
 */
fun ixNotoSansTypography(): Typography {
    val t = TypographyTokens
    val f = ixNotoSansFontFamily
    return Typography(
        headlineLarge = t.fontHeadingXlRegular.copy(fontFamily = f),
        headlineMedium = t.fontHeadingLgRegular.copy(fontFamily = f),
        headlineSmall = t.fontHeadingSmRegular.copy(fontFamily = f),
        titleLarge = t.fontHeadingMdRegular.copy(fontFamily = f),
        titleMedium = t.fontBodyLgMedium.copy(fontFamily = f),
        titleSmall = t.fontBodyMdMedium.copy(fontFamily = f),
        bodyLarge = t.fontBodyLgRegular.copy(fontFamily = f),
        bodyMedium = t.fontBodyMdRegular.copy(fontFamily = f),
        bodySmall = t.fontBodySmRegular.copy(fontFamily = f),
        labelLarge = t.fontBodyMdMedium.copy(fontFamily = f),
        labelMedium = t.fontBodySmMedium.copy(fontFamily = f),
        labelSmall = t.fontBodyXsMedium.copy(fontFamily = f),
    )
}

/**
 * Creates a [TypographyTokens] instance using the Iltix Noto Sans font.
 * This overrides all Compound typography tokens (used via [ElementTheme.typography]).
 */
fun ixNotoSansCompoundTypographyTokens(): TypographyTokens = TypographyTokens(ixNotoSansFontFamily)
