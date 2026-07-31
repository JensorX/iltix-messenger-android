package de.iltix.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import de.iltix.components.roomlist.LocalIxHazeState
import io.element.android.compound.theme.ElementTheme
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
    useGlassTheme: Boolean,
    showNavigationBar: Boolean,
    onSetUpRecoveryClick: () -> Unit,
    onConfirmRecoveryKeyClick: () -> Unit,
    onRoomClick: (RoomListRoomSummary) -> Unit,
    onOpenSpace: (RoomId) -> Unit,
    onCreateRoomClick: () -> Unit,
) {
    val ixSpaceNavShape = RoundedCornerShape(28.dp)
    var spaceNavHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val isDarkTheme = isSystemInDarkTheme()
    val gradientHeight = with(density) { spaceNavHeightPx.toDp() } + outerPadding.calculateBottomPadding() + 8.dp
    val gradientBottomColor = ElementTheme.colors.bgCanvasDefault.copy(alpha = 0.80f)
    val glassBorderColor = if (isDarkTheme) {
        Color.Black.copy(alpha = 0.42f)
    } else {
        Color.White.copy(alpha = 0.68f)
    }
    val glassBackgroundBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
        ),
    )

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
            .then(
                if (useGlassTheme) {
                    Modifier.background(glassBackgroundBrush)
                } else {
                    Modifier
                }
            )
    ) {
        // Layer 1 (bottom): Room list
        CompositionLocalProvider(LocalIxHazeState provides hazeState) {
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
        }
        // Layer 2 (middle): Gradient fade
        if (shouldShowIxSpaceNav) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(gradientHeight.coerceAtLeast(1.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                gradientBottomColor,
                            ),
                        ),
                    ),
            )
            // Layer 3 (top): Space navigation
            IxFloatingSpaceNav(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .then(
                        if (useGlassTheme) {
                            Modifier
                        } else {
                            Modifier.shadow(
                                elevation = 3.dp,
                                shape = ixSpaceNavShape,
                                clip = false,
                            )
                        }
                    )
                    .clip(ixSpaceNavShape)
                    .then(
                        if (useGlassTheme) {
                            Modifier.border(1.dp, glassBorderColor, ixSpaceNavShape)
                        } else {
                            Modifier
                        }
                    )
                    .onSizeChanged { spaceNavHeightPx = it.height }
                    .then(
                        if (useGlassTheme) {
                            Modifier.hazeEffect(
                                state = hazeState,
                                style = HazeMaterials.thick(),
                            )
                        } else {
                            Modifier
                        }
                    ),
                state = roomListState.spaceFiltersState,
                onNavigateToSpace = onOpenSpace,
                useGlassTheme = useGlassTheme,
            )
        }
    }
}
