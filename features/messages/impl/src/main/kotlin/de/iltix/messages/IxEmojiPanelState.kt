package de.iltix.messages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import io.element.android.features.messages.impl.messagecomposer.MessageComposerState
import io.element.android.libraries.androidutils.ui.hideKeyboard
import io.element.android.libraries.androidutils.ui.showKeyboard
import kotlinx.coroutines.launch

data class IxEmojiPanelState(
    val emojiPickerEnabled: Boolean,
    val showEmojiPanel: Boolean,
    val panelHeight: Dp,
    val showEmojiButton: Boolean,
    val applyImePadding: Boolean,
    val onToggleEmojiPanel: () -> Unit,
    val hideEmojiPanel: () -> Unit,
)

@Composable
fun rememberIxEmojiPanelState(
    composerState: MessageComposerState,
    isIltixBuild: Boolean,
): IxEmojiPanelState {
    val appContext = LocalContext.current.applicationContext
    val ixPreferencesStore = remember(isIltixBuild, appContext) {
        if (isIltixBuild) IxPreferencesStore(appContext) else null
    }
    val emojiPickerEnabled by remember(ixPreferencesStore) {
        ixPreferencesStore?.settingFlow(IxPrefs.EMOJI_PICKER)
    }?.collectAsState(initial = IxPrefs.EMOJI_PICKER.defaultValue) ?: remember {
        mutableStateOf(false)
    }

    var showEmojiPanel by rememberSaveable { mutableStateOf(false) }

    val localView = LocalView.current
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val imeBottomDp: Dp = with(density) { imeBottomPx.toDp() }

    var lastImeHeight by remember { mutableStateOf(308.dp) }
    // Track previous IME height to detect growth direction.
    // Only update lastImeHeight when the keyboard is expanding (not while dismissing),
    // so we always preserve the fully-extended height.
    var prevImeBottomDp by remember { mutableStateOf(0.dp) }
    if (imeBottomDp >= 120.dp && !showEmojiPanel && imeBottomDp > prevImeBottomDp) {
        lastImeHeight = imeBottomDp
    }
    prevImeBottomDp = imeBottomDp

    val coroutineScope = rememberCoroutineScope()

    fun toggleEmojiPanel() {
        if (showEmojiPanel) {
            showEmojiPanel = false
            coroutineScope.launch {
                composerState.textEditorState.requestFocus()
                localView.showKeyboard()
            }
        } else {
            localView.clearFocus()
            showEmojiPanel = true
            localView.hideKeyboard()
        }
    }

    LaunchedEffect(showEmojiPanel, imeBottomDp, composerState.textEditorState.hasFocus()) {
        if (showEmojiPanel && imeBottomDp > 0.dp && composerState.textEditorState.hasFocus()) {
            showEmojiPanel = false
        }
    }

    val showEmojiButton = emojiPickerEnabled

    return IxEmojiPanelState(
        emojiPickerEnabled = emojiPickerEnabled,
        showEmojiPanel = showEmojiPanel,
        panelHeight = lastImeHeight.coerceAtLeast(220.dp),
        showEmojiButton = showEmojiButton,
        applyImePadding = !showEmojiPanel,
        onToggleEmojiPanel = ::toggleEmojiPanel,
        hideEmojiPanel = { showEmojiPanel = false },
    )
}
