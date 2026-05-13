/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.spaces.SpaceServiceFilter
import io.element.android.libraries.matrix.ui.model.getAvatarData

@Composable
fun IxSpaceNavBar(
    filters: List<SpaceServiceFilter>,
    selectedSpaceId: RoomId?,
    clearLabel: String,
    onClearSelection: () -> Unit,
    onSelectFilter: (SpaceServiceFilter) -> Unit,
    onOpenSpace: (SpaceServiceFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (filters.isEmpty()) return

    val containerShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .clip(containerShape)
            .background(Color.Transparent)
            .border(
                width = 1.dp,
                color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.14f),
                shape = containerShape,
            )
    ) {
        Row(
            modifier = Modifier
                .padding(1.dp)
                .clip(RoundedCornerShape(27.dp))
                .padding(horizontal = 14.dp)
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IxSpaceNavItem(
                label = clearLabel,
                isSelected = selectedSpaceId == null,
                onClick = onClearSelection,
            )
            filters.forEach { filter ->
                IxSpaceNavItem(
                    label = filter.spaceRoom.displayName,
                    avatar = {
                        Avatar(
                            avatarData = filter.spaceRoom.getAvatarData(AvatarSize.RoomSelectRoomListItem),
                            avatarType = AvatarType.Space(),
                        )
                    },
                    isSelected = selectedSpaceId == filter.spaceRoom.roomId,
                    onClick = { onSelectFilter(filter) },
                    onLongClick = { onOpenSpace(filter) },
                )
            }
        }
    }
}

@Composable
private fun IxSpaceNavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    avatar: @Composable (() -> Unit)? = null,
) {
    val backgroundColor = if (isSelected) {
        ElementTheme.colors.bgActionPrimaryRest.copy(alpha = 0.18f)
    } else {
        Color.Transparent
    }
    val borderColor = if (isSelected) {
        ElementTheme.colors.borderFocused
    } else {
        Color.Transparent
    }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (avatar != null) {
            avatar()
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ElementTheme.colors.bgSubtleSecondary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label.take(1),
                    style = ElementTheme.typography.fontBodySmMedium,
                    color = ElementTheme.colors.textPrimary,
                )
            }
        }
        if (avatar != null) {
            Text(
                text = label,
                style = ElementTheme.typography.fontBodySmMedium,
                color = if (isSelected) ElementTheme.colors.textPrimary else ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.defaultMinSize(minWidth = 0.dp),
            )
        }
    }
}