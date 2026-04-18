package de.iltix.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import io.element.android.features.home.impl.components.RoomListContentView
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.roomlist.RoomListState
import io.element.android.libraries.matrix.api.core.RoomId

@Composable
fun IxHomeChatsContent(
    roomListState: RoomListState,
    roomsLazyListState: LazyListState,
    outerPadding: PaddingValues,
    contentPadding: PaddingValues,
    hazeState: HazeState,
    shouldShowIxSpaceNav: Boolean,
    showNavigationBar: Boolean,
    onSetUpRecoveryClick: () -> Unit,
    onConfirmRecoveryKeyClick: () -> Unit,
    onRoomClick: (RoomListRoomSummary) -> Unit,
    onOpenSpace: (RoomId) -> Unit,
    onCreateRoomClick: () -> Unit,
) {
    val ixSpaceNavShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                PaddingValues(
                    start = outerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = outerPadding.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = outerPadding.calculateBottomPadding(),
                    top = outerPadding.calculateTopPadding(),
                )
            )
            .consumeWindowInsets(outerPadding)
    ) {
        RoomListContentView(
            contentState = roomListState.contentState,
            filtersState = roomListState.filtersState,
            spaceFiltersState = roomListState.spaceFiltersState,
            lazyListState = roomsLazyListState,
            hideInvitesAvatars = roomListState.hideInvitesAvatars,
            eventSink = roomListState.eventSink,
            onSetUpRecoveryClick = onSetUpRecoveryClick,
            onConfirmRecoveryKeyClick = onConfirmRecoveryKeyClick,
            onRoomClick = onRoomClick,
            onCreateRoomClick = onCreateRoomClick,
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        )
        if (shouldShowIxSpaceNav) {
            IxFloatingSpaceNav(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = if (showNavigationBar && !shouldShowIxSpaceNav) 88.dp else 6.dp)
                    .clip(ixSpaceNavShape)
                    .hazeEffect(
                        state = hazeState,
                        style = HazeMaterials.thick(),
                    ),
                state = roomListState.spaceFiltersState,
                onNavigateToSpace = onOpenSpace,
            )
        }
    }
}
