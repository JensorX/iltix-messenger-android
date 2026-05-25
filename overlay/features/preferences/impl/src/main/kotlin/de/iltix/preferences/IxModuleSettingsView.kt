/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.preferences

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.iltix.lib.R
import de.iltix.lib.preferences.IxPrefs
import io.element.android.libraries.designsystem.components.preferences.DropdownOption
import io.element.android.libraries.designsystem.components.preferences.PreferenceCategory
import io.element.android.libraries.designsystem.components.preferences.PreferenceDropdown
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.components.preferences.PreferenceSwitch
import io.element.android.libraries.designsystem.components.preferences.PreferenceTextField
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.compound.theme.ElementTheme
import kotlinx.collections.immutable.toImmutableList

@Composable
fun IxModuleSettingsView(
    state: IxModuleSettingsState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Intercept system back when inside a submenu — go to Main, not all the way up
    BackHandler(enabled = state.currentView != IxSettingsView.Main) {
        state.eventSink(IxModuleSettingsEvents.NavigateToView(IxSettingsView.Main))
    }
    when (state.currentView) {
        IxSettingsView.Main -> IxModuleSettingsMainView(state, onBackClick, modifier)
        IxSettingsView.General -> IxModuleSettingsGeneralView(state, modifier)
        IxSettingsView.Overview -> IxModuleSettingsOverviewView(state, modifier)
        IxSettingsView.Chat -> IxModuleSettingsChatView(state, modifier)
        IxSettingsView.Theme -> IxModuleSettingsThemeView(state, modifier)
    }
}

@Composable
private fun IxModuleSettingsMainView(
    state: IxModuleSettingsState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferencePage(
        title = stringResource(id = de.iltix.lib.R.string.iltix_modules_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        // Master controls — no section label needed, page title is already "Iltix Modules"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { state.eventSink(IxModuleSettingsEvents.DeactivateAllModules) },
                text = stringResource(id = R.string.iltix_modules_action_deactivate_all),
                size = ButtonSize.MediumLowPadding,
            )
            Button(
                modifier = Modifier.weight(1f),
                onClick = { state.eventSink(IxModuleSettingsEvents.ResetToDefaults) },
                text = stringResource(id = R.string.iltix_modules_action_reset_defaults),
                size = ButtonSize.MediumLowPadding,
            )
        }

        // Category navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    state.eventSink(IxModuleSettingsEvents.NavigateToView(IxSettingsView.General))
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.iltix_modules_category_general_title),
                style = MaterialTheme.typography.bodyLarge,
                color = ElementTheme.colors.textPrimary,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    state.eventSink(IxModuleSettingsEvents.NavigateToView(IxSettingsView.Overview))
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.iltix_modules_category_overview_title),
                style = MaterialTheme.typography.bodyLarge,
                color = ElementTheme.colors.textPrimary,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    state.eventSink(IxModuleSettingsEvents.NavigateToView(IxSettingsView.Chat))
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.iltix_modules_category_chat_title),
                style = MaterialTheme.typography.bodyLarge,
                color = ElementTheme.colors.textPrimary,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    state.eventSink(IxModuleSettingsEvents.NavigateToView(IxSettingsView.Theme))
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.iltix_modules_category_theme_title),
                style = MaterialTheme.typography.bodyLarge,
                color = ElementTheme.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun IxModuleSettingsGeneralView(
    state: IxModuleSettingsState,
    modifier: Modifier = Modifier,
) {
    PreferencePage(
        title = stringResource(id = R.string.iltix_modules_category_general_title),
        onBackClick = { state.eventSink(IxModuleSettingsEvents.NavigateToView(IxSettingsView.Main)) },
        modifier = modifier,
    ) {
        IxPrefs.LOCAL_USERNAMES.titleRes?.let { titleRes ->
            PreferenceSwitch(
                title = stringResource(id = titleRes),
                subtitle = IxPrefs.LOCAL_USERNAMES.summaryRes?.let { stringResource(id = it) },
                isChecked = state.preferencesValues["LOCAL_USERNAMES"] ?: IxPrefs.LOCAL_USERNAMES.defaultValue,
                onCheckedChange = { enabled ->
                    state.eventSink(IxModuleSettingsEvents.ToggleModule("LOCAL_USERNAMES", enabled))
                },
            )
        }

        IxPrefs.PRIORITY_NOTIFICATION.titleRes?.let { titleRes ->
            PreferenceSwitch(
                title = stringResource(id = titleRes),
                subtitle = IxPrefs.PRIORITY_NOTIFICATION.summaryRes?.let { stringResource(id = it) },
                isChecked = state.preferencesValues["PRIORITY_NOTIFICATION"] ?: IxPrefs.PRIORITY_NOTIFICATION.defaultValue,
                onCheckedChange = { enabled ->
                    state.eventSink(IxModuleSettingsEvents.ToggleModule("PRIORITY_NOTIFICATION", enabled))
                },
            )
        }

        PreferenceCategory(title = stringResource(id = R.string.iltix_modules_auto_download_section_title)) {
            val mediaAutoDownloadEnabled = state.preferencesValues["MEDIA_AUTO_DOWNLOAD"]
                ?: IxPrefs.MEDIA_AUTO_DOWNLOAD.defaultValue

            IxPrefs.MEDIA_AUTO_DOWNLOAD.titleRes?.let { titleRes ->
                PreferenceSwitch(
                    title = stringResource(id = titleRes),
                    subtitle = IxPrefs.MEDIA_AUTO_DOWNLOAD.summaryRes?.let { stringResource(id = it) },
                    isChecked = mediaAutoDownloadEnabled,
                    onCheckedChange = { enabled ->
                        state.eventSink(IxModuleSettingsEvents.ToggleModule("MEDIA_AUTO_DOWNLOAD", enabled))
                    },
                )
            }

            run {
                data class NetworkOption(val value: String, val label: String) : DropdownOption {
                    @Composable
                    override fun getText(): String = label
                }
                val networkOptions = listOf(
                    NetworkOption("wifi_and_mobile", stringResource(id = R.string.iltix_media_auto_download_network_wifi_and_mobile)),
                    NetworkOption("wifi_only", stringResource(id = R.string.iltix_media_auto_download_network_wifi_only)),
                ).toImmutableList()
                val currentMode = state.stringValues["MEDIA_AUTO_DOWNLOAD_NETWORK_MODE"]
                    ?: IxPrefs.MEDIA_AUTO_DOWNLOAD_NETWORK_MODE.defaultValue
                val selectedOption = networkOptions.firstOrNull { it.value == currentMode }
                IxPrefs.MEDIA_AUTO_DOWNLOAD_NETWORK_MODE.titleRes?.let { titleRes ->
                    PreferenceDropdown(
                        title = stringResource(id = titleRes),
                        supportingText = IxPrefs.MEDIA_AUTO_DOWNLOAD_NETWORK_MODE.summaryRes?.let { stringResource(id = it) },
                        selectedOption = selectedOption,
                        options = networkOptions,
                        enabled = mediaAutoDownloadEnabled,
                        onSelectOption = { option ->
                            state.eventSink(IxModuleSettingsEvents.SetStringModule("MEDIA_AUTO_DOWNLOAD_NETWORK_MODE", option.value))
                        },
                    )
                }
            }

            IxPrefs.MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY.titleRes?.let { titleRes ->
                PreferenceSwitch(
                    title = stringResource(id = titleRes),
                    subtitle = IxPrefs.MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY.summaryRes?.let { stringResource(id = it) },
                    isChecked = state.preferencesValues["MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY"]
                        ?: IxPrefs.MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY.defaultValue,
                    enabled = mediaAutoDownloadEnabled,
                    onCheckedChange = { enabled ->
                        state.eventSink(IxModuleSettingsEvents.ToggleModule("MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY", enabled))
                    },
                )
            }

            IxPrefs.MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB.titleRes?.let { titleRes ->
                val currentLimit = state.intValues["MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB"]
                    ?: IxPrefs.MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB.defaultValue
                PreferenceTextField(
                    headline = stringResource(id = titleRes),
                    value = currentLimit.toString(),
                    supportingText = IxPrefs.MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB.summaryRes?.let { stringResource(id = it) },
                    placeholder = "20",
                    enabled = mediaAutoDownloadEnabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    validation = { value -> value?.toIntOrNull()?.let { it in 1..4096 } == true },
                    onValidationErrorMessage = stringResource(R.string.iltix_media_auto_download_mobile_video_limit_invalid),
                    onChange = { value ->
                        val parsedValue = value?.toIntOrNull() ?: return@PreferenceTextField
                        state.eventSink(IxModuleSettingsEvents.SetIntModule("MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB", parsedValue))
                    },
                )
            }
        }
    }
}

@Composable
private fun IxModuleSettingsOverviewView(
    state: IxModuleSettingsState,
    modifier: Modifier = Modifier,
) {
    PreferencePage(
        title = stringResource(id = R.string.iltix_modules_category_overview_title),
        onBackClick = { state.eventSink(IxModuleSettingsEvents.NavigateToView(IxSettingsView.Main)) },
        modifier = modifier,
    ) {
        IxPrefs.SPACE_NAV_MODE.titleRes?.let { titleRes ->
            val currentMode = state.stringValues["SPACE_NAV_MODE"] ?: IxPrefs.SPACE_NAV_MODE.defaultValue
            PreferenceSwitch(
                title = stringResource(id = titleRes),
                subtitle = IxPrefs.SPACE_NAV_MODE.summaryRes?.let { stringResource(id = it) },
                isChecked = currentMode != "none",
                onCheckedChange = { enabled ->
                    state.eventSink(
                        IxModuleSettingsEvents.SetStringModule(
                            moduleKey = "SPACE_NAV_MODE",
                            value = if (enabled) "combined" else "none",
                        )
                    )
                },
            )
        }

        IxPrefs.NUMBER_BADGE.titleRes?.let { titleRes ->
            PreferenceSwitch(
                title = stringResource(id = titleRes),
                subtitle = IxPrefs.NUMBER_BADGE.summaryRes?.let { stringResource(id = it) },
                isChecked = state.preferencesValues["NUMBER_BADGE"] ?: IxPrefs.NUMBER_BADGE.defaultValue,
                onCheckedChange = { enabled ->
                    state.eventSink(IxModuleSettingsEvents.ToggleModule("NUMBER_BADGE", enabled))
                },
            )
        }

        IxPrefs.START_BUTTON_IN_TOOLBAR.titleRes?.let { titleRes ->
            PreferenceSwitch(
                title = stringResource(id = titleRes),
                subtitle = IxPrefs.START_BUTTON_IN_TOOLBAR.summaryRes?.let { stringResource(id = it) },
                isChecked = state.preferencesValues["START_BUTTON_IN_TOOLBAR"] ?: IxPrefs.START_BUTTON_IN_TOOLBAR.defaultValue,
                onCheckedChange = { enabled ->
                    state.eventSink(IxModuleSettingsEvents.ToggleModule("START_BUTTON_IN_TOOLBAR", enabled))
                },
            )
        }

        IxPrefs.PIN_FAVORITES.titleRes?.let { titleRes ->
            PreferenceSwitch(
                title = stringResource(id = titleRes),
                subtitle = IxPrefs.PIN_FAVORITES.summaryRes?.let { stringResource(id = it) },
                isChecked = state.preferencesValues["PIN_FAVORITES"] ?: IxPrefs.PIN_FAVORITES.defaultValue,
                onCheckedChange = { enabled ->
                    state.eventSink(IxModuleSettingsEvents.ToggleModule("PIN_FAVORITES", enabled))
                },
            )
        }
    }
}

@Composable
private fun IxModuleSettingsChatView(
    state: IxModuleSettingsState,
    modifier: Modifier = Modifier,
) {
    PreferencePage(
        title = stringResource(id = R.string.iltix_modules_category_chat_title),
        onBackClick = { state.eventSink(IxModuleSettingsEvents.NavigateToView(IxSettingsView.Main)) },
        modifier = modifier,
    ) {
        IxPrefs.EMOJI_PICKER.titleRes?.let { titleRes ->
            PreferenceSwitch(
                title = stringResource(id = titleRes),
                subtitle = IxPrefs.EMOJI_PICKER.summaryRes?.let { stringResource(id = it) },
                isChecked = state.preferencesValues["EMOJI_PICKER"] ?: IxPrefs.EMOJI_PICKER.defaultValue,
                onCheckedChange = { enabled ->
                    state.eventSink(IxModuleSettingsEvents.ToggleModule("EMOJI_PICKER", enabled))
                },
            )
        }

        IxPrefs.POLL_VOTE_VIEWER.titleRes?.let { titleRes ->
            PreferenceSwitch(
                title = stringResource(id = titleRes),
                subtitle = IxPrefs.POLL_VOTE_VIEWER.summaryRes?.let { stringResource(id = it) },
                isChecked = state.preferencesValues["POLL_VOTE_VIEWER"] ?: IxPrefs.POLL_VOTE_VIEWER.defaultValue,
                onCheckedChange = { enabled ->
                    state.eventSink(IxModuleSettingsEvents.ToggleModule("POLL_VOTE_VIEWER", enabled))
                },
            )
        }

        IxPrefs.UNENCRYPTED_TOPBAR_ICON.titleRes?.let { titleRes ->
            PreferenceSwitch(
                title = stringResource(id = titleRes),
                subtitle = IxPrefs.UNENCRYPTED_TOPBAR_ICON.summaryRes?.let { stringResource(id = it) },
                isChecked = state.preferencesValues["UNENCRYPTED_TOPBAR_ICON"] ?: IxPrefs.UNENCRYPTED_TOPBAR_ICON.defaultValue,
                onCheckedChange = { enabled ->
                    state.eventSink(IxModuleSettingsEvents.ToggleModule("UNENCRYPTED_TOPBAR_ICON", enabled))
                },
            )
        }
    }
}

@Composable
private fun IxModuleSettingsThemeView(
    state: IxModuleSettingsState,
    modifier: Modifier = Modifier,
) {
    PreferencePage(
        title = stringResource(id = R.string.iltix_modules_category_theme_title),
        onBackClick = { state.eventSink(IxModuleSettingsEvents.NavigateToView(IxSettingsView.Main)) },
        modifier = modifier,
    ) {
        IxPrefs.ILTIX_THEME_MODE.titleRes?.let { titleRes ->
            data class IltixThemeModeOption(val value: String, val label: String) : DropdownOption {
                @Composable
                override fun getText(): String = label
            }
            val themeModeOptions = listOf(
                IltixThemeModeOption("off", stringResource(id = R.string.iltix_theme_mode_option_off)),
                IltixThemeModeOption("solid", stringResource(id = R.string.iltix_theme_mode_option_solid)),
                IltixThemeModeOption("glass", stringResource(id = R.string.iltix_theme_mode_option_glass)),
            ).toImmutableList()
            val currentThemeMode = state.stringValues["ILTIX_THEME_MODE"] ?: IxPrefs.ILTIX_THEME_MODE.defaultValue
            val selectedThemeMode = themeModeOptions.firstOrNull { it.value == currentThemeMode }
            PreferenceDropdown(
                title = stringResource(id = titleRes),
                supportingText = IxPrefs.ILTIX_THEME_MODE.summaryRes?.let { stringResource(id = it) },
                selectedOption = selectedThemeMode,
                options = themeModeOptions,
                onSelectOption = { option ->
                    state.eventSink(IxModuleSettingsEvents.SetStringModule("ILTIX_THEME_MODE", option.value))
                },
            )
        }

        IxPrefs.MATERIAL_YOU_THEME.titleRes?.let { titleRes ->
            PreferenceSwitch(
                title = stringResource(id = titleRes),
                subtitle = IxPrefs.MATERIAL_YOU_THEME.summaryRes?.let { stringResource(id = it) },
                isChecked = state.preferencesValues["MATERIAL_YOU_THEME"] ?: IxPrefs.MATERIAL_YOU_THEME.defaultValue,
                onCheckedChange = { enabled ->
                    state.eventSink(IxModuleSettingsEvents.ToggleModule("MATERIAL_YOU_THEME", enabled))
                },
            )
        }

        IxPrefs.ROUNDED_BUBBLES.titleRes?.let { titleRes ->
            PreferenceSwitch(
                title = stringResource(id = titleRes),
                subtitle = IxPrefs.ROUNDED_BUBBLES.summaryRes?.let { stringResource(id = it) },
                isChecked = state.preferencesValues["ROUNDED_BUBBLES"] ?: IxPrefs.ROUNDED_BUBBLES.defaultValue,
                onCheckedChange = { enabled ->
                    state.eventSink(IxModuleSettingsEvents.ToggleModule("ROUNDED_BUBBLES", enabled))
                },
            )
        }

        IxPrefs.CARD_ROOM_ROWS.titleRes?.let { titleRes ->
            data class CardRoomRowsOption(val value: String, val label: String) : DropdownOption {
                @Composable
                override fun getText(): String = label
            }
            val cardRoomRowsOptions = listOf(
                CardRoomRowsOption("connected", stringResource(id = R.string.iltix_card_room_rows_option_connected)),
                CardRoomRowsOption("cards", stringResource(id = R.string.iltix_card_room_rows_option_cards)),
                CardRoomRowsOption("none", stringResource(id = R.string.iltix_card_room_rows_option_none)),
            ).toImmutableList()
            val currentMode = state.stringValues["CARD_ROOM_ROWS"] ?: IxPrefs.CARD_ROOM_ROWS.defaultValue
            val selectedOption = cardRoomRowsOptions.firstOrNull { it.value == currentMode }
            PreferenceDropdown(
                title = stringResource(id = titleRes),
                supportingText = IxPrefs.CARD_ROOM_ROWS.summaryRes?.let { stringResource(id = it) },
                selectedOption = selectedOption,
                options = cardRoomRowsOptions,
                onSelectOption = { option ->
                    state.eventSink(IxModuleSettingsEvents.SetStringModule("CARD_ROOM_ROWS", option.value))
                },
            )
        }

        run {
            data class FontOption(val value: String, val label: String) : DropdownOption {
                @Composable
                override fun getText(): String = label
            }
            val fontOptions = listOf(
                FontOption("noto_sans", stringResource(id = R.string.iltix_font_option_noto_sans)),
                FontOption("roboto_condensed", stringResource(id = R.string.iltix_font_option_roboto_condensed)),
                FontOption("system", stringResource(id = R.string.iltix_font_option_system)),
            ).toImmutableList()
            val currentFont = state.stringValues["ILTIX_FONT"] ?: IxPrefs.ILTIX_FONT.defaultValue
            val selectedFont = fontOptions.firstOrNull { it.value == currentFont }
            IxPrefs.ILTIX_FONT.titleRes?.let { titleRes ->
                PreferenceDropdown(
                    title = stringResource(id = titleRes),
                    supportingText = IxPrefs.ILTIX_FONT.summaryRes?.let { stringResource(id = it) },
                    selectedOption = selectedFont,
                    options = fontOptions,
                    onSelectOption = { option ->
                        state.eventSink(IxModuleSettingsEvents.SetStringModule("ILTIX_FONT", option.value))
                    },
                )
            }
        }
    }
}
