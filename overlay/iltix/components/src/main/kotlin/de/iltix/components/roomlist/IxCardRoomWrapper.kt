/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.components.roomlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Wraps any room-list row composable with optional filled card appearances.
 *
 * Border intentionally removed — the filled background provides sufficient
 * visual separation from the canvas.
 *
 * This component lives entirely within the Iltix Modules so that the upstream
 * [RoomSummaryRow] remains unmodified. [RoomListContentView] passes the mode
 * from [de.iltix.lib.preferences.IxPrefs.CARD_ROOM_ROWS].
 */
@Composable
fun IxCardRoomWrapper(
    mode: String,
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(verticalPadding)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        content()
    }
}
