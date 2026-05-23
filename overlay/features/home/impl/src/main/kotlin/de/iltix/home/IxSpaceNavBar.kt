/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.spaces.SpaceServiceFilter
import io.element.android.libraries.matrix.ui.model.getAvatarData

/** The rounded corner radius for the pill/capsule highlight and the container. */
private val ContainerShape = RoundedCornerShape(28.dp)
private val CapsuleShape = RoundedCornerShape(50)

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

    Box(
        modifier = modifier
            .clip(ContainerShape)
            .background(
                ElementTheme.colors.bgCanvasDefault.copy(alpha = 0.72f)
            ),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // "All Chats" overview item with Home icon
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
    // Animate selection factor (0f → unselected, 1f → selected)
    val selectionFactor by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "selectionFactor",
    )

    // Spring-bounce scale for the icon — mimics GlassTabView's "pop" on selection
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "iconScale",
    )

    // Capsule highlight colours (9% accent at full selection — matches Telegram)
    val capsuleAlpha = selectionFactor * 0.09f
    val capsuleColor = ElementTheme.colors.textActionPrimary.copy(alpha = capsuleAlpha)

    // Icon / text tint
    val contentColor = lerp(
        start = ElementTheme.colors.textSecondary,
        stop = ElementTheme.colors.textActionPrimary,
        fraction = selectionFactor,
    )

    // Item alpha
    val contentAlpha = lerp(start = 0.6f, stop = 1f, fraction = selectionFactor)

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Capsule / icon area
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 28.dp)
                    .clip(CapsuleShape)
                    .background(capsuleColor),
                contentAlignment = Alignment.Center,
            ) {
                if (avatar != null) {
                    // Space avatar — scale it for the spring pop
                    Box(modifier = Modifier.scale(iconScale * 0.5f)) {
                        avatar()
                    }
                } else {
                    // Home icon for "All Chats"
                    val homeIcon = if (isSelected) CompoundIcons.HomeSolid() else CompoundIcons.Home()
                    Icon(
                        imageVector = homeIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .size(13.dp)
                            .scale(iconScale),
                    )
                }
            }

            // Label below icon
            Text(
                text = label,
                style = ElementTheme.typography.fontBodyXsMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor.copy(alpha = contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Linear interpolation between two [Color] values by [fraction].
 */
private fun lerp(start: Color, stop: Color, fraction: Float): Color = Color(
    red = start.red + (stop.red - start.red) * fraction,
    green = start.green + (stop.green - start.green) * fraction,
    blue = start.blue + (stop.blue - start.blue) * fraction,
    alpha = start.alpha + (stop.alpha - start.alpha) * fraction,
)

/**
 * Linear interpolation between two [Float] values by [fraction].
 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction