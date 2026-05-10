/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.components.poll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import de.iltix.components.nicknames.rememberIxResolvedDisplayName
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.SimpleModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.ui.strings.CommonPlurals

@Composable
fun IxPollVoteViewerSheet(
    answerText: String,
    voters: List<UserId>,
    onDismiss: () -> Unit,
) {
    SimpleModalBottomSheet(
        title = answerText,
        onDismiss = onDismiss,
    ) {
        Text(
            text = pluralStringResource(
                id = CommonPlurals.common_poll_votes_count,
                count = voters.size,
                voters.size,
            ),
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            voters.forEach { voter ->
                IxVoterItem(voter)
            }
        }
    }
}

@Composable
private fun IxVoterItem(voter: UserId) {
    val displayName = rememberIxResolvedDisplayName(
        userId = voter.value,
        fallbackName = voter.value,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = displayName ?: voter.value,
            style = ElementTheme.typography.fontBodyMdMedium,
            color = ElementTheme.colors.textPrimary,
        )
        if (displayName != null && displayName != voter.value) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = voter.value,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
            )
        }
    }
}