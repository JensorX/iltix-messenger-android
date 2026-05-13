package de.iltix.home

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import de.iltix.lib.R
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

    val hierarchy = remember(filters) { buildIxSpaceHierarchy(filters) }
    val selectedPath = remember(hierarchy.parentByChild, state) {
        buildSelectedPath(
            selectedId = state.selectedFilter()?.spaceRoom?.roomId,
            parentByChild = hierarchy.parentByChild,
        )
    }

    fun requestSelection(target: SpaceServiceFilter?) {
        when (state) {
            is SpaceFiltersState.Selecting -> {
                if (target == null) {
                    state.eventSink(SpaceFiltersEvent.Selecting.Cancel)
                } else {
                    state.eventSink(SpaceFiltersEvent.Selecting.SelectFilter(target))
                }
            }
            is SpaceFiltersState.Selected -> {
                if (target == null || target.spaceRoom.roomId == state.selectedFilter.spaceRoom.roomId) {
                    state.eventSink(SpaceFiltersEvent.Selected.ClearSelection)
                } else {
                    pendingSelection = target
                    state.eventSink(SpaceFiltersEvent.Selected.ClearSelection)
                }
            }
            is SpaceFiltersState.Unselected -> {
                pendingSelection = target
                state.eventSink(SpaceFiltersEvent.Unselected.ShowFilters)
            }
            SpaceFiltersState.Disabled -> Unit
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IxSpaceNavBar(
            filters = hierarchy.rootFilters,
            selectedSpaceId = selectedPath.firstOrNull(),
            clearLabel = stringResource(id = R.string.iltix_space_nav_all_label),
            onClearSelection = { requestSelection(null) },
            onSelectFilter = { requestSelection(it) },
            onOpenSpace = { filter -> onNavigateToSpace(filter.spaceRoom.roomId) },
        )

        selectedPath.forEachIndexed { index, parentId ->
            val children = hierarchy.childrenByParent[parentId].orEmpty()
            if (children.isEmpty()) return@forEachIndexed
            val selectedChild = selectedPath.getOrNull(index + 1)
            val parentFilter = hierarchy.filtersById[parentId]
            IxSpaceNavBar(
                filters = children,
                selectedSpaceId = selectedChild,
                clearLabel = stringResource(id = R.string.iltix_space_nav_all_label),
                onClearSelection = {
                    if (parentFilter != null) {
                        requestSelection(parentFilter)
                    } else {
                        requestSelection(null)
                    }
                },
                onSelectFilter = { requestSelection(it) },
                onOpenSpace = { filter -> onNavigateToSpace(filter.spaceRoom.roomId) },
            )
        }
    }
}

private data class IxSpaceHierarchy(
    val rootFilters: List<SpaceServiceFilter>,
    val filtersById: Map<RoomId, SpaceServiceFilter>,
    val parentByChild: Map<RoomId, RoomId>,
    val childrenByParent: Map<RoomId, List<SpaceServiceFilter>>,
)

private fun buildIxSpaceHierarchy(filters: List<SpaceServiceFilter>): IxSpaceHierarchy {
    val byId = filters.associateBy { it.spaceRoom.roomId }
    val minLevel = filters.minOfOrNull { it.level }
    if (minLevel == null) {
        return IxSpaceHierarchy(
            rootFilters = emptyList(),
            filtersById = emptyMap(),
            parentByChild = emptyMap(),
            childrenByParent = emptyMap(),
        )
    }

    val roots = filters.filter { it.level == minLevel }
    val parentByChild = mutableMapOf<RoomId, RoomId>()
    val childrenByParent = mutableMapOf<RoomId, MutableList<SpaceServiceFilter>>()

    filters.forEach { child ->
        if (child.level == minLevel) return@forEach
        val parent = filters.firstOrNull { candidate ->
            candidate.level == child.level - 1 && candidate.descendants.contains(child.spaceRoom.roomId)
        } ?: return@forEach
        parentByChild[child.spaceRoom.roomId] = parent.spaceRoom.roomId
        childrenByParent.getOrPut(parent.spaceRoom.roomId) { mutableListOf() }.add(child)
    }

    return IxSpaceHierarchy(
        rootFilters = roots,
        filtersById = byId,
        parentByChild = parentByChild,
        childrenByParent = childrenByParent,
    )
}

private fun buildSelectedPath(
    selectedId: RoomId?,
    parentByChild: Map<RoomId, RoomId>,
): List<RoomId> {
    if (selectedId == null) return emptyList()
    val path = mutableListOf<RoomId>()
    var current: RoomId? = selectedId
    while (current != null) {
        path.add(current)
        current = parentByChild[current]
    }
    return path.asReversed()
}
