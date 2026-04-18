package de.iltix.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import io.element.android.compound.colors.SemanticColorsLightDark
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.theme.Theme
import io.element.android.compound.theme.isDark
import io.element.android.compound.theme.mapToTheme
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.designsystem.theme.LocalBuildMeta
import io.element.android.libraries.preferences.api.store.AppPreferencesStore

@Composable
fun IxElementThemeApp(
    appPreferencesStore: AppPreferencesStore,
    baseSemanticColors: SemanticColorsLightDark,
    buildMeta: BuildMeta,
    content: @Composable () -> Unit,
) {
    val theme by remember {
        appPreferencesStore.getThemeFlow().mapToTheme()
    }.collectAsState(initial = Theme.System)
    LaunchedEffect(theme) {
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }

    val isIltixBuild = buildMeta.applicationId.contains("iltix")
    val ixTheme = rememberIxResolvedTheme(base = baseSemanticColors, isIltixBuild = isIltixBuild)

    CompositionLocalProvider(
        LocalBuildMeta provides buildMeta,
    ) {
        ElementTheme(
            darkTheme = theme.isDark(),
            compoundLight = ixTheme.semanticColors.light,
            compoundDark = ixTheme.semanticColors.dark,
            typography = ixTheme.typography.materialTypography,
            typographyTokens = ixTheme.typography.typographyTokens,
        ) {
            ProvideIxStyleSettings(settings = ixTheme.settings) {
                content()
            }
        }
    }
}