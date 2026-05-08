/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.topbars

import de.iltix.components.nicknames.rememberIxResolvedDisplayName
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.avatar.anAvatarData
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.preview.ROOM_NAME
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.matrix.ui.components.aMatrixUserList
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThreadTopBar(
    roomName: String?,
    roomAvatarData: AvatarData,
    heroes: ImmutableList<AvatarData>,
    isTombstoned: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val ixPreferencesStore = remember(context) { IxPreferencesStore(context) }
    val useIltixTheme by remember(ixPreferencesStore) {
        ixPreferencesStore.settingFlow(IxPrefs.ILTIX_THEME)
    }.collectAsState(initial = IxPrefs.ILTIX_THEME.defaultValue)

    val localNicknameUserId = heroes.singleOrNull()?.id
    val resolvedRoomName = rememberIxResolvedDisplayName(
        userId = localNicknameUserId,
        fallbackName = roomName,
    )
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            BackButton(onClick = onBackClick)
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    avatarData = roomAvatarData,
                    avatarType = AvatarType.Room(
                        heroes = heroes,
                        isTombstoned = isTombstoned,
                    ),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .semantics {
                            heading()
                        },
                ) {
                    Text(
                        text = stringResource(CommonStrings.common_thread),
                        style = ElementTheme.typography.fontBodyLgMedium,
                    )
                    Text(
                        text = resolvedRoomName ?: stringResource(CommonStrings.common_no_room_name),
                        style = ElementTheme.typography.fontBodySmRegular,
                        fontStyle = FontStyle.Italic.takeIf { resolvedRoomName == null },
                        color = ElementTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (useIltixTheme) ElementTheme.colors.bgCanvasDefault else Color.Transparent,
            scrolledContainerColor = if (useIltixTheme) ElementTheme.colors.bgCanvasDefault else Color.Transparent,
        ),
    )
}

@PreviewsDayNight
@Composable
internal fun ThreadTopBarPreview() = ElementPreview {
    @Composable
    fun AThreadTopBar(
        roomName: String? = ROOM_NAME,
        roomAvatarData: AvatarData = anAvatarData(
            name = ROOM_NAME,
            size = AvatarSize.TimelineRoom,
        ),
        isTombstoned: Boolean = false,
        heroes: ImmutableList<AvatarData> = persistentListOf(),
    ) = ThreadTopBar(
        roomName = roomName,
        roomAvatarData = roomAvatarData,
        isTombstoned = isTombstoned,
        heroes = heroes,
        onBackClick = {},
    )
    Column {
        AThreadTopBar()
        HorizontalDivider()
        AThreadTopBar(
            heroes = aMatrixUserList().map { it.getAvatarData(AvatarSize.TimelineRoom) }.toImmutableList(),
        )
        HorizontalDivider()
        AThreadTopBar(
            roomName = null,
        )
        HorizontalDivider()
        AThreadTopBar(
            roomAvatarData = anAvatarData(
                name = ROOM_NAME,
                url = "https://some-avatar.jpg",
                size = AvatarSize.TimelineRoom,
            ),
        )
        HorizontalDivider()
        AThreadTopBar(
            isTombstoned = true,
        )
    }
}
