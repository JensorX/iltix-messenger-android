/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.components.roomlist

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.theme.components.Icon

/**
 * Small star icon shown next to the room name for favourite rooms.
 * Belongs entirely within the Iltix Modules — integrated into upstream via a
 * single conditional call in [RoomSummaryRow].
 */
@Composable
fun IxFavoriteStarIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = CompoundIcons.FavouriteSolid(),
        contentDescription = null,
        tint = ElementTheme.colors.iconAccentPrimary,
        modifier = modifier.size(14.dp),
    )
}
