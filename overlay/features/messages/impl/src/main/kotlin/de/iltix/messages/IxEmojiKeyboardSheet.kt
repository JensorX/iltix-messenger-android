/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

/**
 * Inline emoji keyboard panel that occupies the same height as the software keyboard.
 * Use instead of IxEmojiKeyboardSheet for IME-like behavior (keyboard replacement, not overlay).
 */
@Composable
fun IxEmojiKeyboardPanel(
    recentEmojis: ImmutableList<String>,
    panelHeight: Dp,
    onSelectEmoji: (String) -> Unit,
) {
    val fallbackEmojis = listOf("😀", "😂", "😍", "👍", "🙏", "🔥", "🎉", "❤️")
    val emojiItems = (recentEmojis.toList() + fallbackEmojis).distinct().take(40)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(emojiItems) { emoji ->
                Surface(
                    onClick = { onSelectEmoji(emoji) },
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = emoji,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

}
