/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.messages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.element.android.emojibasebindings.EmojibaseStore
import io.element.android.features.messages.impl.timeline.components.customreaction.picker.EmojiPicker
import io.element.android.features.messages.impl.timeline.components.customreaction.picker.EmojiPickerPresenter
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentSetOf

/**
 * Inline emoji keyboard panel that occupies the same height as the software keyboard.
 * Use instead of IxEmojiKeyboardSheet for IME-like behavior (keyboard replacement, not overlay).
 */
@Composable
fun IxEmojiKeyboardPanel(
    emojibaseStore: EmojibaseStore,
    recentEmojis: ImmutableList<String>,
    panelHeight: Dp,
    onSelectEmoji: (String) -> Unit,
) {
    val presenter = remember(emojibaseStore, recentEmojis) {
        EmojiPickerPresenter(
            emojibaseStore = emojibaseStore,
            recentEmojis = recentEmojis,
            coroutineDispatchers = CoroutineDispatchers.Default,
        )
    }
    EmojiPicker(
        onSelectEmoji = { emoji -> onSelectEmoji(emoji.unicode) },
        state = presenter.present(),
        selectedEmojis = persistentSetOf(),
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight),
    )
}
