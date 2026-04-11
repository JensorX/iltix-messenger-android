/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import de.iltix.lib.R
import io.element.android.compound.tokens.CompoundTypographyTokens
import io.element.android.compound.tokens.defaultCompoundTypographyTokens

private val ixRobotoFontFamily = FontFamily(
    Font(resId = R.font.roboto_condensed_regular, weight = FontWeight.W400),
    Font(resId = R.font.roboto_condensed_medium, weight = FontWeight.W500),
    Font(resId = R.font.roboto_condensed_bold, weight = FontWeight.W700),
    Font(resId = R.font.roboto_condensed_italic, weight = FontWeight.W400, style = FontStyle.Italic),
    Font(resId = R.font.roboto_condensed_medium_italic, weight = FontWeight.W500, style = FontStyle.Italic),
    Font(resId = R.font.roboto_condensed_bold_italic, weight = FontWeight.W700, style = FontStyle.Italic),
)

fun ixRobotoTypographyTokens(): CompoundTypographyTokens {
    val base = defaultCompoundTypographyTokens
    return base.copy(
        fontBodyLgMedium = base.fontBodyLgMedium.copy(fontFamily = ixRobotoFontFamily),
        fontBodyLgRegular = base.fontBodyLgRegular.copy(fontFamily = ixRobotoFontFamily),
        fontBodyMdMedium = base.fontBodyMdMedium.copy(fontFamily = ixRobotoFontFamily),
        fontBodyMdRegular = base.fontBodyMdRegular.copy(fontFamily = ixRobotoFontFamily),
        fontBodySmMedium = base.fontBodySmMedium.copy(fontFamily = ixRobotoFontFamily),
        fontBodySmRegular = base.fontBodySmRegular.copy(fontFamily = ixRobotoFontFamily),
        fontBodyXsMedium = base.fontBodyXsMedium.copy(fontFamily = ixRobotoFontFamily),
        fontBodyXsRegular = base.fontBodyXsRegular.copy(fontFamily = ixRobotoFontFamily),
        fontHeadingLgBold = base.fontHeadingLgBold.copy(fontFamily = ixRobotoFontFamily),
        fontHeadingLgRegular = base.fontHeadingLgRegular.copy(fontFamily = ixRobotoFontFamily),
        fontHeadingMdBold = base.fontHeadingMdBold.copy(fontFamily = ixRobotoFontFamily),
        fontHeadingMdRegular = base.fontHeadingMdRegular.copy(fontFamily = ixRobotoFontFamily),
        fontHeadingSmMedium = base.fontHeadingSmMedium.copy(fontFamily = ixRobotoFontFamily),
        fontHeadingSmRegular = base.fontHeadingSmRegular.copy(fontFamily = ixRobotoFontFamily),
        fontHeadingXlBold = base.fontHeadingXlBold.copy(fontFamily = ixRobotoFontFamily),
        fontHeadingXlRegular = base.fontHeadingXlRegular.copy(fontFamily = ixRobotoFontFamily),
    )
}
