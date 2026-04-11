package de.iltix.messages

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.iltix.theme.LocalIxBubbleStyle
import io.element.android.libraries.designsystem.theme.LocalBuildMeta

data class IxVoiceMessageUiConfig(
    val useRoundedVoiceUi: Boolean,
)

@Composable
fun rememberIxVoiceMessageUiConfig(): IxVoiceMessageUiConfig {
    val isIltixBuild = LocalBuildMeta.current.applicationId.contains("iltix")
    val cornerRadius = if (isIltixBuild) {
        LocalIxBubbleStyle.current.cornerRadius
    } else {
        12.dp
    }
    val roundedVoiceUiEnabled = isIltixBuild && cornerRadius > 12.dp

    return IxVoiceMessageUiConfig(
        useRoundedVoiceUi = roundedVoiceUiEnabled,
    )
}
