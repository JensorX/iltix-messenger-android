package de.iltix.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.movableContentOf
import io.element.android.compound.colors.SemanticColorsLightDark
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.theme.Theme
import io.element.android.compound.theme.mapToTheme
import io.element.android.compound.tokens.generated.SemanticColors
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.designsystem.theme.LocalBuildMeta
import io.element.android.libraries.featureflag.api.FeatureFlagService
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.preferences.api.store.AppPreferencesStore

@Composable
fun IxElementThemeApp(
    appPreferencesStore: AppPreferencesStore,
    featureFlagService: FeatureFlagService,
    compoundLight: SemanticColors,
    compoundDark: SemanticColors,
    buildMeta: BuildMeta,
    content: @Composable () -> Unit,
) {
    val isBlackThemeAllowed by remember {
        featureFlagService.isFeatureEnabledFlow(FeatureFlags.AllowBlackTheme)
    }.collectAsState(initial = false)

    val theme by remember(isBlackThemeAllowed) {
        appPreferencesStore.getThemeFlow().mapToTheme(allowBlackTheme = isBlackThemeAllowed)
    }.collectAsState(initial = Theme.System)

    LaunchedEffect(theme) {
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                Theme.Dark, Theme.Black -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }

    val isIltixBuild = buildMeta.applicationId.contains("iltix")
    val baseSemanticColors = SemanticColorsLightDark(light = compoundLight, dark = compoundDark)
    val ixTheme = rememberIxResolvedTheme(base = baseSemanticColors, isIltixBuild = isIltixBuild)

    val ixTypography = ixTheme.typography.materialTypography

    val themedContent = remember {
        movableContentOf {
            ProvideIxStyleSettings(settings = ixTheme.settings) {
                content()
            }
        }
    }

    CompositionLocalProvider(
        LocalBuildMeta provides buildMeta,
    ) {
        if (ixTypography != null) {
            ElementTheme(
                theme = theme,
                compoundLight = ixTheme.semanticColors.light,
                compoundDark = ixTheme.semanticColors.dark,
                typography = ixTypography,
                content = themedContent,
            )
        } else {
            ElementTheme(
                theme = theme,
                compoundLight = ixTheme.semanticColors.light,
                compoundDark = ixTheme.semanticColors.dark,
                content = themedContent,
            )
        }
    }
}