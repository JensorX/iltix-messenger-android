/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import io.element.android.libraries.architecture.Presenter
import kotlinx.coroutines.launch

class IxModuleSettingsPresenter : Presenter<IxModuleSettingsState> {

    @Composable
    override fun present(): IxModuleSettingsState {
        val context = LocalContext.current.applicationContext
        val preferencesStore = remember(context) { IxPreferencesStore(context) }
        val coroutineScope = rememberCoroutineScope()
        var currentView by remember { mutableStateOf(IxSettingsView.Main) }

        // Collect all preferences into a map
        val preferencesMap = mutableMapOf<String, Boolean>()
        val loadingMap = mutableMapOf<String, Boolean>()
        val stringValuesMap = mutableMapOf<String, String>()
        val intValuesMap = mutableMapOf<String, Int>()

        // Collect each preference
        val localUsernames by preferencesStore.settingFlow(IxPrefs.LOCAL_USERNAMES)
            .collectAsState(initial = IxPrefs.LOCAL_USERNAMES.defaultValue)
        preferencesMap["LOCAL_USERNAMES"] = localUsernames

        val spaceNavMode by preferencesStore.settingFlow(IxPrefs.SPACE_NAV_MODE)
            .collectAsState(initial = IxPrefs.SPACE_NAV_MODE.defaultValue)
        stringValuesMap["SPACE_NAV_MODE"] = spaceNavMode

        val numberBadge by preferencesStore.settingFlow(IxPrefs.NUMBER_BADGE)
            .collectAsState(initial = IxPrefs.NUMBER_BADGE.defaultValue)
        preferencesMap["NUMBER_BADGE"] = numberBadge

        val startButtonInToolbar by preferencesStore.settingFlow(IxPrefs.START_BUTTON_IN_TOOLBAR)
            .collectAsState(initial = IxPrefs.START_BUTTON_IN_TOOLBAR.defaultValue)
        preferencesMap["START_BUTTON_IN_TOOLBAR"] = startButtonInToolbar

        val pinFavorites by preferencesStore.settingFlow(IxPrefs.PIN_FAVORITES)
            .collectAsState(initial = IxPrefs.PIN_FAVORITES.defaultValue)
        preferencesMap["PIN_FAVORITES"] = pinFavorites

        val showTypingInOverview by preferencesStore.settingFlow(IxPrefs.SHOW_TYPING_IN_OVERVIEW)
            .collectAsState(initial = IxPrefs.SHOW_TYPING_IN_OVERVIEW.defaultValue)
        preferencesMap["SHOW_TYPING_IN_OVERVIEW"] = showTypingInOverview

        val iltixThemeMode by preferencesStore.settingFlow(IxPrefs.ILTIX_THEME_MODE)
            .collectAsState(initial = IxPrefs.ILTIX_THEME_MODE.defaultValue)
        stringValuesMap["ILTIX_THEME_MODE"] = iltixThemeMode

        val materialYouTheme by preferencesStore.settingFlow(IxPrefs.MATERIAL_YOU_THEME)
            .collectAsState(initial = IxPrefs.MATERIAL_YOU_THEME.defaultValue)
        preferencesMap["MATERIAL_YOU_THEME"] = materialYouTheme

        val roundedBubbles by preferencesStore.settingFlow(IxPrefs.ROUNDED_BUBBLES)
            .collectAsState(initial = IxPrefs.ROUNDED_BUBBLES.defaultValue)
        preferencesMap["ROUNDED_BUBBLES"] = roundedBubbles

        val cardRoomRows by preferencesStore.settingFlow(IxPrefs.CARD_ROOM_ROWS)
            .collectAsState(initial = IxPrefs.CARD_ROOM_ROWS.defaultValue)
        stringValuesMap["CARD_ROOM_ROWS"] = cardRoomRows

        val iltixFont by preferencesStore.settingFlow(IxPrefs.ILTIX_FONT)
            .collectAsState(initial = IxPrefs.ILTIX_FONT.defaultValue)
        stringValuesMap["ILTIX_FONT"] = iltixFont

        val emojiPicker by preferencesStore.settingFlow(IxPrefs.EMOJI_PICKER)
            .collectAsState(initial = IxPrefs.EMOJI_PICKER.defaultValue)
        preferencesMap["EMOJI_PICKER"] = emojiPicker

        val pollVoteViewer by preferencesStore.settingFlow(IxPrefs.POLL_VOTE_VIEWER)
            .collectAsState(initial = IxPrefs.POLL_VOTE_VIEWER.defaultValue)
        preferencesMap["POLL_VOTE_VIEWER"] = pollVoteViewer

        val priorityNotification by preferencesStore.settingFlow(IxPrefs.PRIORITY_NOTIFICATION)
            .collectAsState(initial = IxPrefs.PRIORITY_NOTIFICATION.defaultValue)
        preferencesMap["PRIORITY_NOTIFICATION"] = priorityNotification

        val unencryptedTopbarIcon by preferencesStore.settingFlow(IxPrefs.UNENCRYPTED_TOPBAR_ICON)
            .collectAsState(initial = IxPrefs.UNENCRYPTED_TOPBAR_ICON.defaultValue)
        preferencesMap["UNENCRYPTED_TOPBAR_ICON"] = unencryptedTopbarIcon

        val mediaAutoDownload by preferencesStore.settingFlow(IxPrefs.MEDIA_AUTO_DOWNLOAD)
            .collectAsState(initial = IxPrefs.MEDIA_AUTO_DOWNLOAD.defaultValue)
        preferencesMap["MEDIA_AUTO_DOWNLOAD"] = mediaAutoDownload

        val mediaAutoDownloadNetworkMode by preferencesStore.settingFlow(IxPrefs.MEDIA_AUTO_DOWNLOAD_NETWORK_MODE)
            .collectAsState(initial = IxPrefs.MEDIA_AUTO_DOWNLOAD_NETWORK_MODE.defaultValue)
        stringValuesMap["MEDIA_AUTO_DOWNLOAD_NETWORK_MODE"] = mediaAutoDownloadNetworkMode

        val mediaAutoDownloadVideosWifiOnly by preferencesStore.settingFlow(IxPrefs.MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY)
            .collectAsState(initial = IxPrefs.MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY.defaultValue)
        preferencesMap["MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY"] = mediaAutoDownloadVideosWifiOnly

        val mediaAutoDownloadMobileVideoLimit by preferencesStore.settingFlow(IxPrefs.MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB)
            .collectAsState(initial = IxPrefs.MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB.defaultValue)
        intValuesMap["MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB"] = mediaAutoDownloadMobileVideoLimit

        fun handleEvents(event: IxModuleSettingsEvents) {
            when (event) {
                is IxModuleSettingsEvents.ToggleModule -> {
                    coroutineScope.launch {
                        val pref = when (event.moduleKey) {
                            "LOCAL_USERNAMES" -> IxPrefs.LOCAL_USERNAMES
                            "NUMBER_BADGE" -> IxPrefs.NUMBER_BADGE
                            "START_BUTTON_IN_TOOLBAR" -> IxPrefs.START_BUTTON_IN_TOOLBAR
                            "PIN_FAVORITES" -> IxPrefs.PIN_FAVORITES
                            "SHOW_TYPING_IN_OVERVIEW" -> IxPrefs.SHOW_TYPING_IN_OVERVIEW
                            "MATERIAL_YOU_THEME" -> IxPrefs.MATERIAL_YOU_THEME
                            "ROUNDED_BUBBLES" -> IxPrefs.ROUNDED_BUBBLES
                            "EMOJI_PICKER" -> IxPrefs.EMOJI_PICKER
                            "POLL_VOTE_VIEWER" -> IxPrefs.POLL_VOTE_VIEWER
                            "PRIORITY_NOTIFICATION" -> IxPrefs.PRIORITY_NOTIFICATION
                            "UNENCRYPTED_TOPBAR_ICON" -> IxPrefs.UNENCRYPTED_TOPBAR_ICON
                            "MEDIA_AUTO_DOWNLOAD" -> IxPrefs.MEDIA_AUTO_DOWNLOAD
                            "MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY" -> IxPrefs.MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY
                            else -> return@launch
                        }
                        preferencesStore.setSetting(pref, event.enabled)
                    }
                }
                is IxModuleSettingsEvents.SetStringModule -> {
                    coroutineScope.launch {
                        val pref = when (event.moduleKey) {
                            "SPACE_NAV_MODE" -> IxPrefs.SPACE_NAV_MODE
                            "CARD_ROOM_ROWS" -> IxPrefs.CARD_ROOM_ROWS
                            "MEDIA_AUTO_DOWNLOAD_NETWORK_MODE" -> IxPrefs.MEDIA_AUTO_DOWNLOAD_NETWORK_MODE
                            "ILTIX_THEME_MODE" -> IxPrefs.ILTIX_THEME_MODE
                            "ILTIX_FONT" -> IxPrefs.ILTIX_FONT
                            else -> return@launch
                        }
                        preferencesStore.setSetting(pref, event.value)
                    }
                }
                is IxModuleSettingsEvents.SetIntModule -> {
                    coroutineScope.launch {
                        val pref = when (event.moduleKey) {
                            "MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB" -> IxPrefs.MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB
                            else -> return@launch
                        }
                        preferencesStore.setSetting(pref, event.value)
                    }
                }
                is IxModuleSettingsEvents.NavigateToView -> {
                    currentView = event.view
                }
                IxModuleSettingsEvents.DeactivateAllModules -> {
                    coroutineScope.launch {
                        preferencesStore.setSetting(IxPrefs.LOCAL_USERNAMES, false)
                        preferencesStore.setSetting(IxPrefs.SPACE_NAV_MODE, "none")
                        preferencesStore.setSetting(IxPrefs.NUMBER_BADGE, false)
                        preferencesStore.setSetting(IxPrefs.START_BUTTON_IN_TOOLBAR, false)
                        preferencesStore.setSetting(IxPrefs.PIN_FAVORITES, false)
                        preferencesStore.setSetting(IxPrefs.SHOW_TYPING_IN_OVERVIEW, false)
                        preferencesStore.setSetting(IxPrefs.EMOJI_PICKER, false)
                        preferencesStore.setSetting(IxPrefs.POLL_VOTE_VIEWER, false)
                        preferencesStore.setSetting(IxPrefs.PRIORITY_NOTIFICATION, false)
                        preferencesStore.setSetting(IxPrefs.UNENCRYPTED_TOPBAR_ICON, false)
                        preferencesStore.setSetting(IxPrefs.MEDIA_AUTO_DOWNLOAD, false)
                        preferencesStore.setSetting(IxPrefs.ILTIX_THEME_MODE, "off")
                        preferencesStore.setSetting(IxPrefs.MATERIAL_YOU_THEME, false)
                        preferencesStore.setSetting(IxPrefs.ROUNDED_BUBBLES, false)
                        preferencesStore.setSetting(IxPrefs.CARD_ROOM_ROWS, "none")
                        preferencesStore.setSetting(IxPrefs.ILTIX_FONT, "system")
                    }
                }
                IxModuleSettingsEvents.ResetToDefaults -> {
                    coroutineScope.launch {
                        preferencesStore.setSetting(IxPrefs.LOCAL_USERNAMES, IxPrefs.LOCAL_USERNAMES.defaultValue)
                        preferencesStore.setSetting(IxPrefs.SPACE_NAV_MODE, IxPrefs.SPACE_NAV_MODE.defaultValue)
                        preferencesStore.setSetting(IxPrefs.NUMBER_BADGE, IxPrefs.NUMBER_BADGE.defaultValue)
                        preferencesStore.setSetting(IxPrefs.START_BUTTON_IN_TOOLBAR, IxPrefs.START_BUTTON_IN_TOOLBAR.defaultValue)
                        preferencesStore.setSetting(IxPrefs.PIN_FAVORITES, IxPrefs.PIN_FAVORITES.defaultValue)
                        preferencesStore.setSetting(IxPrefs.SHOW_TYPING_IN_OVERVIEW, IxPrefs.SHOW_TYPING_IN_OVERVIEW.defaultValue)
                        preferencesStore.setSetting(IxPrefs.EMOJI_PICKER, IxPrefs.EMOJI_PICKER.defaultValue)
                        preferencesStore.setSetting(IxPrefs.POLL_VOTE_VIEWER, IxPrefs.POLL_VOTE_VIEWER.defaultValue)
                        preferencesStore.setSetting(IxPrefs.PRIORITY_NOTIFICATION, IxPrefs.PRIORITY_NOTIFICATION.defaultValue)
                        preferencesStore.setSetting(IxPrefs.UNENCRYPTED_TOPBAR_ICON, IxPrefs.UNENCRYPTED_TOPBAR_ICON.defaultValue)
                        preferencesStore.setSetting(IxPrefs.MEDIA_AUTO_DOWNLOAD, IxPrefs.MEDIA_AUTO_DOWNLOAD.defaultValue)
                        preferencesStore.setSetting(IxPrefs.MEDIA_AUTO_DOWNLOAD_NETWORK_MODE, IxPrefs.MEDIA_AUTO_DOWNLOAD_NETWORK_MODE.defaultValue)
                        preferencesStore.setSetting(IxPrefs.MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY, IxPrefs.MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY.defaultValue)
                        preferencesStore.setSetting(IxPrefs.MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB, IxPrefs.MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB.defaultValue)
                        preferencesStore.setSetting(IxPrefs.ILTIX_THEME_MODE, IxPrefs.ILTIX_THEME_MODE.defaultValue)
                        preferencesStore.setSetting(IxPrefs.MATERIAL_YOU_THEME, IxPrefs.MATERIAL_YOU_THEME.defaultValue)
                        preferencesStore.setSetting(IxPrefs.ROUNDED_BUBBLES, IxPrefs.ROUNDED_BUBBLES.defaultValue)
                        preferencesStore.setSetting(IxPrefs.CARD_ROOM_ROWS, IxPrefs.CARD_ROOM_ROWS.defaultValue)
                        preferencesStore.setSetting(IxPrefs.ILTIX_FONT, IxPrefs.ILTIX_FONT.defaultValue)
                    }
                }
            }
        }

        return IxModuleSettingsState(
            preferencesLoading = loadingMap,
            preferencesValues = preferencesMap,
            stringValues = stringValuesMap,
            intValues = intValuesMap,
            currentView = currentView,
            eventSink = ::handleEvents,
        )
    }
}
