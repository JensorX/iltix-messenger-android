package de.iltix.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemVoiceContent
import io.element.android.libraries.designsystem.atomic.atoms.PlaybackSpeedButton
import io.element.android.libraries.designsystem.components.media.WaveformPlaybackView
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.libraries.ui.utils.a11y.isTalkbackActive
import io.element.android.libraries.voiceplayer.api.VoiceMessageEvent
import io.element.android.libraries.voiceplayer.api.VoiceMessageState
import kotlinx.coroutines.delay

@Composable
fun IxVoiceMessageBody(
    state: VoiceMessageState,
    content: TimelineItemVoiceContent,
    onPlayPause: () -> Unit,
) {
    val voiceMessageUiConfig = rememberIxVoiceMessageUiConfig()
    if (voiceMessageUiConfig.useRoundedVoiceUi) {
        IxVoiceMessageView(
            state = state,
            waveform = content.waveform,
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isTalkbackActive()) {
                when (state.buttonType) {
                    VoiceMessageState.ButtonType.Play -> PlayButton(onClick = onPlayPause)
                    VoiceMessageState.ButtonType.Pause -> PauseButton(onClick = onPlayPause)
                    VoiceMessageState.ButtonType.Downloading -> ProgressButton()
                    VoiceMessageState.ButtonType.Retry -> RetryButton(onClick = onPlayPause)
                    VoiceMessageState.ButtonType.Disabled -> PlayButton(onClick = {}, enabled = false)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PlaybackSpeedButton(
                    speed = state.playbackSpeed,
                    onClick = { state.eventSink(VoiceMessageEvent.ChangePlaybackSpeed) },
                )
                Text(
                    text = state.time,
                    color = ElementTheme.colors.textSecondary,
                    style = ElementTheme.typography.fontBodySmMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            WaveformPlaybackView(
                showCursor = state.showCursor,
                playbackProgress = state.progress,
                waveform = content.waveform,
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp),
                seekEnabled = !isTalkbackActive(),
                onSeek = { state.eventSink(VoiceMessageEvent.Seek(it)) },
            )
        }
    }
}

@Composable
private fun PlayButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    CustomIconButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        ControlIcon(
            imageVector = CompoundIcons.PlaySolid(),
            contentDescription = stringResource(id = CommonStrings.a11y_play),
        )
    }
}

@Composable
private fun PauseButton(
    onClick: () -> Unit,
) {
    CustomIconButton(
        onClick = onClick,
    ) {
        ControlIcon(
            imageVector = CompoundIcons.PauseSolid(),
            contentDescription = stringResource(id = CommonStrings.a11y_pause),
        )
    }
}

@Composable
private fun RetryButton(
    onClick: () -> Unit,
) {
    CustomIconButton(
        onClick = onClick,
    ) {
        ControlIcon(
            imageVector = CompoundIcons.Restart(),
            contentDescription = stringResource(id = CommonStrings.action_retry),
        )
    }
}

@Composable
private fun ControlIcon(
    imageVector: ImageVector,
    contentDescription: String?,
) {
    Icon(
        modifier = Modifier.padding(vertical = 10.dp),
        imageVector = imageVector,
        contentDescription = contentDescription,
    )
}

@Composable
private fun ProgressButton(
    displayImmediately: Boolean = false,
) {
    var canDisplay by remember { mutableStateOf(displayImmediately) }
    LaunchedEffect(Unit) {
        delay(2000L)
        canDisplay = true
    }
    CustomIconButton(
        onClick = {},
        enabled = false,
    ) {
        if (canDisplay) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(2.dp)
                    .size(16.dp),
                color = ElementTheme.colors.iconSecondary,
                strokeWidth = 2.dp,
            )
        } else {
            ControlIcon(
                imageVector = CompoundIcons.PauseSolid(),
                contentDescription = stringResource(id = CommonStrings.a11y_pause),
            )
        }
    }
}

@Composable
private fun CustomIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = ElementTheme.colors.iconPrimary,
            disabledContentColor = ElementTheme.colors.iconSecondary,
        ),
        modifier = Modifier
            .size(40.dp)
            .background(
                color = ElementTheme.colors.bgSubtleSecondary,
                shape = CircleShape,
            ),
        content = content,
    )
}
