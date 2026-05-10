package de.iltix.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import io.element.android.features.home.impl.HomeNavigationBarItem
import io.element.android.features.home.impl.spacefilters.SpaceFiltersEvent
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.features.home.impl.spacefilters.selectedFilter
import io.element.android.features.home.impl.roomlist.RoomListState
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.spaces.SpaceServiceFilter

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
        roomListState.spaceFiltersState !is SpaceFiltersState.Disabled

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
    // Remember the available filters so they persist across state transitions
    var rememberedFilters by remember { mutableStateOf<List<SpaceServiceFilter>>(emptyList()) }
    if (state is SpaceFiltersState.Selecting) {
        rememberedFilters = state.availableFilters
    }

    // Pending selection to apply after cycling back to Selecting
    var pendingSelection by remember { mutableStateOf<SpaceServiceFilter?>(null) }

    // Auto-transition to Selecting state to get the filter list
    LaunchedEffect(state) {
        if (state is SpaceFiltersState.Unselected) {
            state.eventSink(SpaceFiltersEvent.Unselected.ShowFilters)
        }
    }

    // Apply pending selection when we reach Selecting state
    LaunchedEffect(state, pendingSelection) {
        if (state is SpaceFiltersState.Selecting && pendingSelection != null) {
            state.eventSink(SpaceFiltersEvent.Selecting.SelectFilter(pendingSelection!!))
            pendingSelection = null
        }
    }

    val filters = when (state) {
        is SpaceFiltersState.Selecting -> state.availableFilters
        else -> rememberedFilters
    }
    if (filters.isEmpty()) return

    IxSpaceNavBar(
        modifier = modifier,
        filters = filters,
        selectedSpaceId = state.selectedFilter()?.spaceRoom?.roomId,
        onClearSelection = {
            when (state) {
                is SpaceFiltersState.Selected -> state.eventSink(SpaceFiltersEvent.Selected.ClearSelection)
                is SpaceFiltersState.Selecting -> state.eventSink(SpaceFiltersEvent.Selecting.Cancel)
                else -> Unit
            }
        },
        onSelectFilter = { filter ->
            when (state) {
                is SpaceFiltersState.Selecting -> state.eventSink(SpaceFiltersEvent.Selecting.SelectFilter(filter))
                is SpaceFiltersState.Selected -> {
                    if (filter.spaceRoom.roomId == state.selectedFilter.spaceRoom.roomId) {
                        // Tapping the already-selected filter → deselect
                        state.eventSink(SpaceFiltersEvent.Selected.ClearSelection)
                    } else {
                        pendingSelection = filter
                        state.eventSink(SpaceFiltersEvent.Selected.ClearSelection)
                    }
                }
                else -> Unit
            }
        },
        onOpenSpace = { filter -> onNavigateToSpace(filter.spaceRoom.roomId) },
    )
}
