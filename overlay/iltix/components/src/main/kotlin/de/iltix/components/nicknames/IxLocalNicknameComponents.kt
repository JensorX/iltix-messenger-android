/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.components.nicknames

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.iltix.lib.R
import de.iltix.lib.nicknames.IxLocalNicknameStore
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.OutlinedButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf

@Composable
fun rememberIxResolvedDisplayName(
    userId: String?,
    fallbackName: String?,
): String? {
    val context = LocalContext.current.applicationContext
    val preferencesStore = remember(context) { IxPreferencesStore(context) }
    val nicknamesEnabled by remember(preferencesStore) {
        preferencesStore.settingFlow(IxPrefs.LOCAL_USERNAMES)
    }.collectAsState(initial = IxPrefs.LOCAL_USERNAMES.defaultValue)
    val nicknameStore = remember(context) { IxLocalNicknameStore(context) }
    val nicknameFlow = remember(userId, nicknameStore, nicknamesEnabled) {
        if (!nicknamesEnabled || userId.isNullOrBlank()) {
            flowOf(null)
        } else {
            nicknameStore.nicknameFlow(userId)
        }
    }
    val nickname by nicknameFlow.collectAsState(initial = null)
    return nickname ?: fallbackName
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IxLocalNicknameAction(
    userId: String,
    fallbackName: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val preferencesStore = remember(context) { IxPreferencesStore(context) }
    val coroutineScope = rememberCoroutineScope()
    val nicknamesEnabled by remember(preferencesStore) {
        preferencesStore.settingFlow(IxPrefs.LOCAL_USERNAMES)
    }.collectAsState(initial = IxPrefs.LOCAL_USERNAMES.defaultValue)
    if (!nicknamesEnabled) return

    val nicknameStore = remember(context) { IxLocalNicknameStore(context) }
    val existingNickname by remember(userId, nicknameStore) {
        nicknameStore.nicknameFlow(userId)
    }.collectAsState(initial = null)

    var isSheetOpen by rememberSaveable(userId) { mutableStateOf(false) }
    var editedNickname by rememberSaveable(userId, existingNickname) { mutableStateOf(existingNickname.orEmpty()) }

    ListItem(
        modifier = modifier,
        content = {
            Text(
                stringResource(
                    id = if (existingNickname.isNullOrBlank()) {
                        R.string.iltix_local_nickname_set_title
                    } else {
                        R.string.iltix_local_nickname_edit_title
                    }
                )
            )
        },
        supportingContent = {
            Text(existingNickname ?: fallbackName ?: userId)
        },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.UserProfileSolid())),
        onClick = { isSheetOpen = true },
    )

    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            scrollable = false,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.iltix_local_nickname_sheet_title),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = fallbackName ?: userId,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.iltix_local_nickname_field_label),
                    value = editedNickname,
                    placeholder = stringResource(R.string.iltix_local_nickname_field_placeholder),
                    singleLine = true,
                    onValueChange = { editedNickname = it },
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    text = stringResource(CommonStrings.action_save),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        coroutineScope.launch {
                            nicknameStore.setNickname(userId, editedNickname)
                        }
                        isSheetOpen = false
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    text = stringResource(CommonStrings.action_remove),
                    modifier = Modifier.fillMaxWidth(),
                    destructive = true,
                    onClick = {
                        coroutineScope.launch {
                            nicknameStore.setNickname(userId, null)
                        }
                        isSheetOpen = false
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    text = stringResource(CommonStrings.action_cancel),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        editedNickname = existingNickname.orEmpty()
                        isSheetOpen = false
                    },
                )
            }
        }
    }
}
