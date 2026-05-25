/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import io.element.android.compound.tokens.generated.TypographyTokens
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import io.element.android.compound.colors.SemanticColorsLightDark
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.theme.messageFromMeBackground
import io.element.android.libraries.designsystem.theme.messageFromOtherBackground
import kotlinx.coroutines.flow.combine

data class IxThemeSettings(
    val iltixThemeEnabled: Boolean,
    val materialYouEnabled: Boolean,
    val roundedBubblesEnabled: Boolean,
    val iltixFont: String,
)

data class IxTypographyOverrides(
    val materialTypography: Typography?,
    val compoundTypographyTokens: TypographyTokens?,
)

data class IxResolvedTheme(
    val semanticColors: SemanticColorsLightDark,
    val settings: IxThemeSettings,
    val typography: IxTypographyOverrides,
)

data class IxBubbleStyle(
    val cornerRadius: Dp,
    val ownBackgroundColor: Color,
    val otherBackgroundColor: Color,
)

val LocalIxBubbleStyle = staticCompositionLocalOf {
    IxBubbleStyle(
        cornerRadius = 12.dp,
        ownBackgroundColor = Color.Unspecified,
        otherBackgroundColor = Color.Unspecified,
    )
}

@Composable
fun rememberIxThemeSettings(isIltixBuild: Boolean): IxThemeSettings {
    if (!isIltixBuild) {
        return IxThemeSettings(
            iltixThemeEnabled = false,
            materialYouEnabled = false,
            roundedBubblesEnabled = false,
            iltixFont = "system",
        )
    }

    val context = LocalContext.current.applicationContext
    val preferencesStore = remember(context) { IxPreferencesStore(context) }
    val settings by remember(preferencesStore) {
        combine(
            preferencesStore.settingFlow(IxPrefs.ILTIX_THEME),
            preferencesStore.settingFlow(IxPrefs.MATERIAL_YOU_THEME),
            preferencesStore.settingFlow(IxPrefs.ROUNDED_BUBBLES),
            preferencesStore.settingFlow(IxPrefs.ILTIX_FONT),
        ) { iltixThemeEnabled, materialYouEnabled, roundedBubblesEnabled, iltixFont ->
            IxThemeSettings(
                iltixThemeEnabled = iltixThemeEnabled,
                materialYouEnabled = materialYouEnabled,
                roundedBubblesEnabled = roundedBubblesEnabled,
                iltixFont = iltixFont,
            )
        }
    }.collectAsState(
        initial = IxThemeSettings(
            iltixThemeEnabled = true,
            materialYouEnabled = true,
            roundedBubblesEnabled = true,
            iltixFont = IxPrefs.ILTIX_FONT.defaultValue,
        )
    )
    return settings
}

@Composable
fun rememberIxTypographyOverrides(
    settings: IxThemeSettings,
    isIltixBuild: Boolean,
): IxTypographyOverrides {
    val materialTypography = remember(isIltixBuild, settings.iltixFont) {
        if (isIltixBuild) {
            when (settings.iltixFont) {
                "noto_sans" -> ixNotoSansTypography()
                "roboto_condensed" -> ixRobotoTypography()
                else -> null
            }
        } else {
            null
        }
    }
    val compoundTypographyTokens = remember(isIltixBuild, settings.iltixFont) {
        if (isIltixBuild) {
            when (settings.iltixFont) {
                "noto_sans" -> ixNotoSansCompoundTypographyTokens()
                "roboto_condensed" -> ixCompoundTypographyTokens()
                else -> null
            }
        } else {
            null
        }
    }
    return remember(materialTypography, compoundTypographyTokens) {
        IxTypographyOverrides(
            materialTypography = materialTypography,
            compoundTypographyTokens = compoundTypographyTokens,
        )
    }
}

@Composable
fun rememberIxSemanticColors(
    base: SemanticColorsLightDark,
    isIltixBuild: Boolean,
    useIltixTheme: Boolean,
    useMaterialYou: Boolean,
): SemanticColorsLightDark {
    val context = LocalContext.current.applicationContext
    return remember(base, context, isIltixBuild, useIltixTheme, useMaterialYou) {
        if (isIltixBuild && useIltixTheme) {
            IxDynamicColorScheme.semanticColors(context, useMaterialYou)
        } else {
            base
        }
    }
}

@Composable
fun rememberIxResolvedTheme(
    base: SemanticColorsLightDark,
    isIltixBuild: Boolean,
): IxResolvedTheme {
    val settings = rememberIxThemeSettings(isIltixBuild = isIltixBuild)
    val semanticColors = rememberIxSemanticColors(
        base = base,
        isIltixBuild = isIltixBuild,
        useIltixTheme = settings.iltixThemeEnabled,
        useMaterialYou = settings.materialYouEnabled,
    )
    val typography = rememberIxTypographyOverrides(
        settings = settings,
        isIltixBuild = isIltixBuild,
    )
    return remember(semanticColors, settings, typography) {
        IxResolvedTheme(
            semanticColors = semanticColors,
            settings = settings,
            typography = typography,
        )
    }
}

@Composable
fun ProvideIxStyleSettings(
    settings: IxThemeSettings,
    content: @Composable () -> Unit,
) {
    val useIltixTheme = settings.iltixThemeEnabled
    val bubbleStyle = IxBubbleStyle(
        cornerRadius = if (useIltixTheme && settings.roundedBubblesEnabled) 22.dp else 12.dp,
        ownBackgroundColor = if (useIltixTheme && settings.materialYouEnabled) {
            ElementTheme.colors.bgAccentSelected
        } else {
            ElementTheme.colors.messageFromMeBackground
        },
        otherBackgroundColor = if (useIltixTheme && settings.materialYouEnabled) {
            ElementTheme.colors.bgSubtlePrimary
        } else {
            ElementTheme.colors.messageFromOtherBackground
        },
    )
    CompositionLocalProvider(
        LocalIxBubbleStyle provides bubbleStyle,
        content = content,
    )
}