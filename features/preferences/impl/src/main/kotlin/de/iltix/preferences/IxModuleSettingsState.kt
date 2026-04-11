/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.preferences

data class IxModuleSettingsState(
    val preferencesLoading: Map<String, Boolean>,
    val preferencesValues: Map<String, Boolean>,
    val stringValues: Map<String, String> = emptyMap(),
    val intValues: Map<String, Int> = emptyMap(),
    val currentView: IxSettingsView = IxSettingsView.Main,
    val eventSink: (IxModuleSettingsEvents) -> Unit,
)

enum class IxSettingsView {
    Main,
    General,
    Overview,
    Chat,
    Theme,
}

sealed interface IxModuleSettingsEvents {
    data class ToggleModule(val moduleKey: String, val enabled: Boolean) : IxModuleSettingsEvents
    data class SetStringModule(val moduleKey: String, val value: String) : IxModuleSettingsEvents
    data class SetIntModule(val moduleKey: String, val value: Int) : IxModuleSettingsEvents
    data class NavigateToView(val view: IxSettingsView) : IxModuleSettingsEvents
    data object DeactivateAllModules : IxModuleSettingsEvents
    data object ResetToDefaults : IxModuleSettingsEvents
}
