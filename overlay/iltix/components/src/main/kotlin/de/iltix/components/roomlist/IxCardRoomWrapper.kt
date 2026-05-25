/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.components.roomlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

val LocalIxHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Wraps any room-list row composable with optional filled card appearances.
 *
 * Glass mode adds a translucent surface and light/dark border while solid
 * mode keeps the current filled card appearance.
 *
 * This component lives entirely within the Iltix Modules so that the upstream
 * [RoomSummaryRow] remains unmodified. [RoomListContentView] passes the mode
 * from [de.iltix.lib.preferences.IxPrefs.CARD_ROOM_ROWS].
 */
@Composable
fun IxCardRoomWrapper(
    mode: String,
    themeMode: String = "solid",
    index: Int,
    lastIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (mode == "none") {
        content()
        return
    }

    val shape = when (mode) {
        "connected" -> RoundedCornerShape(
            topStart = if (index == 0) 20.dp else 0.dp,
            topEnd = if (index == 0) 20.dp else 0.dp,
            bottomStart = if (index == lastIndex) 20.dp else 0.dp,
            bottomEnd = if (index == lastIndex) 20.dp else 0.dp,
        )
        else -> RoundedCornerShape(20.dp)
    }
    val verticalPadding = when (mode) {
        "connected" -> PaddingValues(
            top = if (index == 0) 4.dp else 0.dp,
            bottom = if (index == lastIndex) 4.dp else 0.dp,
        )
        else -> PaddingValues(vertical = 4.dp)
    }
    val isGlassTheme = themeMode == "glass"
    val hazeState = LocalIxHazeState.current
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isGlassTheme) {
        if (isDarkTheme) Color.Black.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.44f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val borderColor = if (isDarkTheme) {
        Color.Black.copy(alpha = 0.42f)
    } else {
        Color.White.copy(alpha = 0.68f)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(verticalPadding)
            .shadow(
                elevation = if (isGlassTheme) 8.dp else 0.dp,
                shape = shape,
                clip = false,
            )
            .clip(shape)
            .then(
                if (isGlassTheme && hazeState != null) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeMaterials.thin(),
                    )
                } else {
                    Modifier
                }
            )
            .background(backgroundColor)
            .then(
                if (isGlassTheme) {
                    Modifier.border(1.dp, borderColor, shape)
                } else {
                    Modifier
                }
            ),
    ) {
        content()
    }
}
