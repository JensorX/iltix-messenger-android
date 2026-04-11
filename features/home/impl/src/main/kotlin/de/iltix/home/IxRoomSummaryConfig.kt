package de.iltix.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import de.iltix.components.badges.IxUnreadBadge
import de.iltix.components.nicknames.rememberIxResolvedDisplayName
import de.iltix.components.roomlist.IxFavoriteStarIcon
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs

data class IxRoomSummaryConfig(
    val showUnreadCountBadge: Boolean,
    val showFavoriteIndicator: Boolean,
)

@Composable
fun rememberIxRoomSummaryConfig(): IxRoomSummaryConfig {
    val context = LocalContext.current.applicationContext
    val ixPreferencesStore = remember(context) { IxPreferencesStore(context) }
    val showUnreadCountBadge by remember(ixPreferencesStore) {
        ixPreferencesStore.settingFlow(IxPrefs.NUMBER_BADGE)
    }.collectAsState(initial = IxPrefs.NUMBER_BADGE.defaultValue)
    val showFavoriteIndicator by remember(ixPreferencesStore) {
        ixPreferencesStore.settingFlow(IxPrefs.PIN_FAVORITES)
    }.collectAsState(initial = IxPrefs.PIN_FAVORITES.defaultValue)

    return IxRoomSummaryConfig(
        showUnreadCountBadge = showUnreadCountBadge,
        showFavoriteIndicator = showFavoriteIndicator,
    )
}
