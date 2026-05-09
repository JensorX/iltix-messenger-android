/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.element.android.compound.colors.SemanticColorsLightDark
import io.element.android.compound.tokens.generated.SemanticColors
import io.element.android.compound.tokens.generated.compoundColorsDark
import io.element.android.compound.tokens.generated.compoundColorsLight

object IxDynamicColorScheme {
    fun semanticColors(context: Context, useMaterialYou: Boolean): SemanticColorsLightDark {
        val lightScheme = when {
            useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
            else -> honeyLightColorScheme
        }
        val darkScheme = when {
            useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicDarkColorScheme(context)
            else -> honeyDarkColorScheme
        }
        return SemanticColorsLightDark(
            light = compoundColorsLight.withIxColorScheme(lightScheme, isLight = true),
            dark = compoundColorsDark.withIxColorScheme(darkScheme, isLight = false),
        )
    }

    private val honeyLightColorScheme = lightColorScheme(
        primary = Color(0xFFC67C3E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDCC2),
        onPrimaryContainer = Color(0xFF331200),
        secondary = Color(0xFFA68860),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFF3E0),
        onSecondaryContainer = Color(0xFF2B1D08),
        tertiary = Color(0xFF8B7355),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEFDEC9),
        onTertiaryContainer = Color(0xFF221608),
        background = Color(0xFFFFFDF8),
        surface = Color(0xFFFFFDF8),
        surfaceVariant = Color(0xFFE8DDD0),
        onSurface = Color(0xFF1D1B16),
        onSurfaceVariant = Color(0xFF504539),
        outline = Color(0xFF837466),
        outlineVariant = Color(0xFFD4C9BC),
    )

    private val honeyDarkColorScheme = darkColorScheme(
        primary = Color(0xFFFFB74D),
        onPrimary = Color(0xFF4A2000),
        primaryContainer = Color(0xFF6B3A16),
        onPrimaryContainer = Color(0xFFFFDCC2),
        secondary = Color(0xFFD4A574),
        onSecondary = Color(0xFF3B2511),
        secondaryContainer = Color(0xFF543C24),
        onSecondaryContainer = Color(0xFFF0D0A8),
        tertiary = Color(0xFFBFA67A),
        onTertiary = Color(0xFF301E08),
        tertiaryContainer = Color(0xFF49351A),
        onTertiaryContainer = Color(0xFFE0C9A0),
        // OLED-black canvas
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        // Warm dark brown — no green
        surfaceVariant = Color(0xFF2E2418),
        onSurface = Color(0xFFE8DDD0),
        onSurfaceVariant = Color(0xFFD4C9BC),
        outline = Color(0xFF9E8D7E),
        outlineVariant = Color(0xFF4A3D30),
    )
}

private fun SemanticColors.withIxColorScheme(colorScheme: ColorScheme, isLight: Boolean): SemanticColors {
    // Strong Material You tinting – alpha values kept high so the palette colour
    // is clearly visible against the dark OLED surface.
    val subtlePrimary = colorScheme.surfaceVariant.copy(alpha = if (isLight) 0.72f else 0.68f)
    val subtleSecondary = colorScheme.surfaceVariant.copy(alpha = if (isLight) 0.54f else 0.48f)
    val subtleLevel0 = colorScheme.surfaceVariant.copy(alpha = if (isLight) 0.36f else 0.30f)
    // Canvas uses a clearly tinted surface: lerp from OLED/surface towards surfaceVariant.
    val canvasColor = lerp(
        colorScheme.surface,
        colorScheme.surfaceVariant,
        if (isLight) 0.06f else 0.22f,
    )
    val canvasLevel1Color = lerp(
        colorScheme.surface,
        colorScheme.surfaceVariant,
        if (isLight) 0.16f else 0.38f,
    )
    // Use surface color for all gradient stops to eliminate colored gradients
    val transparentSurface = colorScheme.surface.copy(alpha = 0f)
    return copy(
        bgAccentHovered = colorScheme.primaryContainer,
        bgAccentPressed = colorScheme.primary.copy(alpha = 0.92f),
        bgAccentRest = colorScheme.primary,
        bgAccentSelected = colorScheme.primaryContainer,
        bgActionPrimaryDisabled = colorScheme.surfaceVariant.copy(alpha = 0.60f),
        bgActionPrimaryHovered = colorScheme.primaryContainer,
        bgActionPrimaryPressed = colorScheme.primary.copy(alpha = 0.92f),
        bgActionPrimaryRest = colorScheme.primary,
        bgActionSecondaryHovered = colorScheme.secondaryContainer,
        bgActionSecondaryPressed = colorScheme.secondary,
        bgActionSecondaryRest = colorScheme.secondaryContainer,
        bgActionTertiaryHovered = colorScheme.tertiaryContainer,
        bgActionTertiaryRest = colorScheme.tertiaryContainer,
        bgActionTertiarySelected = colorScheme.tertiaryContainer,
        bgBadgeAccent = colorScheme.primaryContainer,
        bgBadgePrimary = colorScheme.primary,
        bgBadgeSecondary = colorScheme.secondaryContainer,
        bgCanvasDefault = canvasColor,
        bgCanvasDefaultLevel1 = canvasLevel1Color,
        bgCanvasDisabled = colorScheme.surfaceVariant.copy(alpha = 0.60f),
        bgInfoSubtle = colorScheme.tertiaryContainer,
        bgSubtlePrimary = subtlePrimary,
        bgSubtleSecondary = subtleSecondary,
        bgSubtleSecondaryLevel0 = subtleLevel0,
        bgSuccessSubtle = colorScheme.secondaryContainer,
        borderAccentSubtle = colorScheme.primary.copy(alpha = 0.30f),
        borderFocused = colorScheme.primary,
        borderInteractiveHovered = colorScheme.primary,
        borderInteractivePrimary = colorScheme.outline,
        borderInteractiveSecondary = colorScheme.outlineVariant,
        // Use transparent surface to eliminate coloured gradients in the TopBar and backgrounds
        gradientActionStop1 = colorScheme.primary,
        gradientActionStop2 = colorScheme.secondary,
        gradientActionStop3 = colorScheme.tertiary,
        gradientActionStop4 = colorScheme.primaryContainer,
        gradientInfoStop1 = colorScheme.secondaryContainer,
        gradientInfoStop2 = colorScheme.tertiaryContainer,
        gradientSubtleStop1 = colorScheme.surface,
        gradientSubtleStop2 = transparentSurface,
        gradientSubtleStop3 = transparentSurface,
        gradientSubtleStop4 = transparentSurface,
        gradientSubtleStop5 = transparentSurface,
        gradientSubtleStop6 = transparentSurface,
        iconAccentPrimary = colorScheme.primary,
        iconAccentTertiary = colorScheme.secondary,
        iconOnSolidPrimary = colorScheme.onPrimary,
        iconPrimary = colorScheme.onSurface,
        iconPrimaryAlpha = colorScheme.onSurface.copy(alpha = 0.72f),
        iconQuaternary = colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
        iconQuaternaryAlpha = colorScheme.onSurfaceVariant.copy(alpha = 0.20f),
        iconSecondary = colorScheme.onSurfaceVariant,
        iconSecondaryAlpha = colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        iconTertiary = colorScheme.onSurfaceVariant.copy(alpha = 0.80f),
        iconTertiaryAlpha = colorScheme.onSurfaceVariant.copy(alpha = 0.54f),
        textActionAccent = colorScheme.primary,
        textActionPrimary = colorScheme.primary,
        textInfoPrimary = colorScheme.tertiary,
        textLinkExternal = colorScheme.primary,
        textOnSolidPrimary = colorScheme.onPrimary,
        textPrimary = colorScheme.onSurface,
        textSecondary = colorScheme.onSurfaceVariant,
        isLight = isLight,
    )
}
