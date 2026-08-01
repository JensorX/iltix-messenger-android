/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.messages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.element.android.libraries.emoji.api.picker.EmojiPickerRenderer
import io.element.android.libraries.emoji.api.picker.EmojiPickerState
import kotlinx.collections.immutable.persistentSetOf

/**
 * Inline emoji keyboard panel that occupies the same height as the software keyboard.
 * Use instead of IxEmojiKeyboardSheet for IME-like behavior (keyboard replacement, not overlay).
 */
@Composable
fun IxEmojiKeyboardPanel(
    pickerState: EmojiPickerState,
    pickerRenderer: EmojiPickerRenderer,
    panelHeight: Dp,
    onSelectEmoji: (String) -> Unit,
) {
    if (!pickerState.isReady) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(panelHeight),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }
    pickerRenderer.Render(
        state = pickerState,
        onSelectEmoji = { emoji -> onSelectEmoji(emoji.unicode) },
        selectedEmojis = persistentSetOf(),
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight),
    )
}
