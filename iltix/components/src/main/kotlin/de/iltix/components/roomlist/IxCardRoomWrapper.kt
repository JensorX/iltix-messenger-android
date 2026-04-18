/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.components.roomlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Wraps any room-list row composable with a filled card appearance:
 * rounded corners and a secondaryContainer background (Material You tinted
 * or warm Honey amber when Material You is disabled).
 *
 * Border intentionally removed — the filled background provides sufficient
 * visual separation from the canvas.
 *
 * This component lives entirely within the Iltix Modules so that the upstream
 * [RoomSummaryRow] remains unmodified. [RoomListContentView] calls it
 * conditionally based on [de.iltix.lib.preferences.IxPrefs.CARD_ROOM_ROWS].
 */
@Composable
fun IxCardRoomWrapper(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        content()
    }
}
