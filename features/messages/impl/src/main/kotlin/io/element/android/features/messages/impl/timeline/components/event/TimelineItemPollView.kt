/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import de.iltix.components.poll.IxPollVoteViewerSheet
import de.iltix.lib.R as IltixR
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import io.element.android.features.messages.impl.timeline.TimelineEvent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemPollContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemPollContentProvider
import io.element.android.features.poll.api.pollcontent.PollContentView
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.LocalBuildMeta
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.collections.immutable.toImmutableList

@Composable
fun TimelineItemPollView(
    content: TimelineItemPollContent,
    eventSink: (TimelineEvent.TimelineItemPollEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun onSelectAnswer(pollStartId: EventId, answerId: String) {
        eventSink(TimelineEvent.SelectPollAnswer(pollStartId, answerId))
    }

    fun onEndPoll(pollStartId: EventId) {
        eventSink(TimelineEvent.EndPoll(pollStartId))
    }

    fun onEditPoll(pollStartId: EventId) {
        eventSink(TimelineEvent.EditPoll(pollStartId))
    }

    val context = LocalContext.current.applicationContext
    val isIltixBuild = LocalBuildMeta.current.applicationId.contains("iltix")
    val ixPreferencesStore = remember(isIltixBuild, context) {
        if (isIltixBuild) IxPreferencesStore(context) else null
    }
    val pollVoteViewerEnabled by remember(ixPreferencesStore) {
        ixPreferencesStore?.settingFlow(IxPrefs.POLL_VOTE_VIEWER)
    }?.collectAsState(initial = IxPrefs.POLL_VOTE_VIEWER.defaultValue) ?: remember {
        mutableStateOf(false)
    }
    var selectedVotes by remember {
        mutableStateOf<Pair<String, List<UserId>>?>(null)
    }

    val viewVotesLabel = if (pollVoteViewerEnabled) {
        stringResource(id = IltixR.string.iltix_poll_view_votes_button)
    } else null

    selectedVotes?.let { (answerText, voters) ->
        IxPollVoteViewerSheet(
            answerText = answerText,
            voters = voters,
            onDismiss = { selectedVotes = null },
        )
    }

    PollContentView(
        eventId = content.eventId,
        question = content.question,
        answerItems = content.answerItems.toImmutableList(),
        pollKind = content.pollKind,
        isPollEnded = content.isEnded,
        isPollEditable = content.isEditable,
        isMine = content.isMine,
        onSelectAnswer = ::onSelectAnswer,
        onEditPoll = ::onEditPoll,
        onEndPoll = ::onEndPoll,
        onViewVotes = if (pollVoteViewerEnabled) { answerItem ->
            selectedVotes = answerItem.answer.text to content.votes[answerItem.answer.id].orEmpty()
        } else null,
        viewVotesLabel = viewVotesLabel,
        modifier = modifier,
    )
}

@PreviewsDayNight
@Composable
internal fun TimelineItemPollViewPreview(@PreviewParameter(TimelineItemPollContentProvider::class) content: TimelineItemPollContent) =
    ElementPreview {
        TimelineItemPollView(
            content = content,
            eventSink = {},
        )
    }
