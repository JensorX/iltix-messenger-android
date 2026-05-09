/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.components.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.theme.components.Text

@Composable
fun IxUnreadBadge(
    count: Long,
    contentDescription: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ElementTheme.colors.bgBadgePrimary,
    contentColor: Color = Color.White,
) {
    val text = if (count > 99) "99+" else count.toString()
    Box(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .background(color = backgroundColor, shape = CircleShape)
            .defaultMinSize(minWidth = 22.dp, minHeight = 32.dp)
            .padding(horizontal = 8.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = ElementTheme.typography.fontBodyXsMedium,
        )
    }
}
