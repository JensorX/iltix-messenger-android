package de.iltix.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import io.element.android.features.home.impl.HomeNavigationBarItem
import io.element.android.features.home.impl.spacefilters.SpaceFiltersEvent
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.features.home.impl.spacefilters.availableFilters
import io.element.android.features.home.impl.spacefilters.selectedFilter
import io.element.android.features.home.impl.roomlist.RoomListState
import io.element.android.libraries.matrix.api.core.RoomId

data class IxHomeUiConfig(
    val showStartChatInTopBar: Boolean,
    val useIltixTheme: Boolean,
    val shouldShowIxSpaceNav: Boolean,
)

@Composable
fun rememberIxHomeUiConfig(
    currentHomeNavigationBarItem: HomeNavigationBarItem,
    roomListState: RoomListState,
): IxHomeUiConfig {
    val context = LocalContext.current.applicationContext
    val ixPreferencesStore = remember(context) { IxPreferencesStore(context) }
    val showStartChatInTopBar by remember(ixPreferencesStore) {
        ixPreferencesStore.settingFlow(IxPrefs.START_BUTTON_IN_TOOLBAR)
    }.collectAsState(initial = IxPrefs.START_BUTTON_IN_TOOLBAR.defaultValue)
    val spaceNavMode by remember(ixPreferencesStore) {
        ixPreferencesStore.settingFlow(IxPrefs.SPACE_NAV_MODE)
    }.collectAsState(initial = IxPrefs.SPACE_NAV_MODE.defaultValue)
    val useIltixTheme by remember(ixPreferencesStore) {
        ixPreferencesStore.settingFlow(IxPrefs.ILTIX_THEME)
    }.collectAsState(initial = IxPrefs.ILTIX_THEME.defaultValue)
    val shouldShowIxSpaceNav = spaceNavMode != "none" &&
        currentHomeNavigationBarItem == HomeNavigationBarItem.Chats &&
        roomListState.spaceFiltersState.availableFilters().isNotEmpty()

    return IxHomeUiConfig(
        showStartChatInTopBar = showStartChatInTopBar,
        useIltixTheme = useIltixTheme,
        shouldShowIxSpaceNav = shouldShowIxSpaceNav,
    )
}

@Composable
fun IxFloatingSpaceNav(
    state: SpaceFiltersState,
    onNavigateToSpace: (RoomId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = state.availableFilters()
    if (filters.isEmpty()) return

    IxSpaceNavBar(
        modifier = modifier,
        filters = filters,
        selectedSpaceId = state.selectedFilter()?.spaceRoom?.roomId,
        onClearSelection = {
            if (state is SpaceFiltersState.Selected) {
                state.eventSink(SpaceFiltersEvent.Selected.ClearSelection)
            }
        },
        onSelectFilter = { filter ->
            when (state) {
                is SpaceFiltersState.Unselected -> state.eventSink(SpaceFiltersEvent.Unselected.SelectFilter(filter))
                is SpaceFiltersState.Selected -> state.eventSink(SpaceFiltersEvent.Selected.SelectFilter(filter))
                is SpaceFiltersState.Selecting -> state.eventSink(SpaceFiltersEvent.Selecting.SelectFilter(filter))
                SpaceFiltersState.Disabled -> Unit
            }
        },
        onOpenSpace = { filter -> onNavigateToSpace(filter.spaceRoom.roomId) },
    )
}
