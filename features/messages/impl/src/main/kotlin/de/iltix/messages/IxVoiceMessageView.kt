/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.atomic.atoms.PlaybackSpeedButton
import io.element.android.libraries.designsystem.components.media.WaveformPlaybackView
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.libraries.voiceplayer.api.VoiceMessageEvent
import io.element.android.libraries.voiceplayer.api.VoiceMessageState
import kotlinx.collections.immutable.ImmutableList

@Composable
fun IxVoiceMessageView(
    state: VoiceMessageState,
    waveform: ImmutableList<Float>,
    modifier: Modifier = Modifier,
) {
    val waveformTrackBrush = SolidColor(ElementTheme.colors.iconPrimaryAlpha)
    val waveformProgressBrush = SolidColor(ElementTheme.colors.iconAccentPrimary)
    val waveformCursorBrush = SolidColor(ElementTheme.colors.iconAccentTertiary)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IxVoiceControlButton(
                buttonType = state.buttonType,
                onClick = { state.eventSink(VoiceMessageEvent.PlayPause) },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.time,
                        modifier = Modifier.weight(1f),
                        color = ElementTheme.colors.textPrimary,
                        style = ElementTheme.typography.fontBodySmMedium,
                    )
                    PlaybackSpeedButton(
                        speed = state.playbackSpeed,
                        onClick = { state.eventSink(VoiceMessageEvent.ChangePlaybackSpeed) },
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = ElementTheme.colors.bgCanvasDefault.copy(alpha = 0.58f),
                            shape = RoundedCornerShape(18.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    WaveformPlaybackView(
                        showCursor = state.showCursor,
                        playbackProgress = state.progress,
                        waveform = waveform,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        seekEnabled = true,
                        onSeek = { state.eventSink(VoiceMessageEvent.Seek(it)) },
                        brush = waveformTrackBrush,
                        progressBrush = waveformProgressBrush,
                        cursorBrush = waveformCursorBrush,
                        lineWidth = 3.dp,
                        linePadding = 2.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun IxVoiceControlButton(
    buttonType: VoiceMessageState.ButtonType,
    onClick: () -> Unit,
) {
    val enabled = buttonType != VoiceMessageState.ButtonType.Disabled && buttonType != VoiceMessageState.ButtonType.Downloading
    val containerColor = if (enabled) {
        ElementTheme.colors.bgAccentRest
    } else {
        ElementTheme.colors.bgCanvasDefault.copy(alpha = 0.75f)
    }
    val contentColor = if (enabled) {
        ElementTheme.colors.iconOnSolidPrimary
    } else {
        ElementTheme.colors.iconDisabled
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .background(color = containerColor, shape = CircleShape)
            .size(42.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = contentColor,
            disabledContentColor = ElementTheme.colors.iconDisabled,
        ),
    ) {
        when (buttonType) {
            VoiceMessageState.ButtonType.Play -> IxVoiceControlIcon(
                imageVector = CompoundIcons.PlaySolid(),
                contentDescription = stringResource(CommonStrings.a11y_play),
            )
            VoiceMessageState.ButtonType.Pause -> IxVoiceControlIcon(
                imageVector = CompoundIcons.PauseSolid(),
                contentDescription = stringResource(CommonStrings.a11y_pause),
            )
            VoiceMessageState.ButtonType.Retry -> IxVoiceControlIcon(
                imageVector = CompoundIcons.Restart(),
                contentDescription = stringResource(CommonStrings.action_retry),
            )
            VoiceMessageState.ButtonType.Downloading -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = ElementTheme.colors.iconPrimary,
                strokeWidth = 2.dp,
            )
            VoiceMessageState.ButtonType.Disabled -> IxVoiceControlIcon(
                imageVector = CompoundIcons.PlaySolid(),
                contentDescription = stringResource(CommonStrings.a11y_play),
            )
        }
    }
}

@Composable
private fun IxVoiceControlIcon(
    imageVector: ImageVector,
    contentDescription: String,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(22.dp),
    )
}