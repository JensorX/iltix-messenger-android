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
    val iltixFontEnabled: Boolean,
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
            iltixFontEnabled = false,
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
        ) { iltixThemeEnabled, materialYouEnabled, roundedBubblesEnabled, iltixFontEnabled ->
            IxThemeSettings(
                iltixThemeEnabled = iltixThemeEnabled,
                materialYouEnabled = materialYouEnabled,
                roundedBubblesEnabled = roundedBubblesEnabled,
                iltixFontEnabled = iltixFontEnabled,
            )
        }
    }.collectAsState(
        initial = IxThemeSettings(
            iltixThemeEnabled = true,
            materialYouEnabled = true,
            roundedBubblesEnabled = true,
            iltixFontEnabled = true,
        )
    )
    return settings
}

@Composable
fun rememberIxTypographyOverrides(
    settings: IxThemeSettings,
    isIltixBuild: Boolean,
): IxTypographyOverrides {
    val materialTypography = remember(isIltixBuild, settings.iltixFontEnabled) {
        if (isIltixBuild && settings.iltixFontEnabled) {
            ixRobotoTypography()
        } else {
            null
        }
    }
    val compoundTypographyTokens = remember(isIltixBuild, settings.iltixFontEnabled) {
        if (isIltixBuild && settings.iltixFontEnabled) {
            ixCompoundTypographyTokens()
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
        cornerRadius = if (useIltixTheme && settings.roundedBubblesEnabled) 28.dp else 12.dp,
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