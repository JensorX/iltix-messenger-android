/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.lib.nicknames

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.ixLocalNicknamesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "iltix_local_nicknames",
)

class IxLocalNicknameStore(
    private val context: Context,
) {
    fun nicknameFlow(userId: String): Flow<String?> {
        val key = stringPreferencesKey(userId)
        return context.ixLocalNicknamesDataStore.data
            .catch { throwable ->
                Timber.w(throwable, "Failed to read Iltix local nicknames, falling back to account display names")
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[key]?.trim()?.takeIf { it.isNotEmpty() }
            }
    }

    suspend fun setNickname(userId: String, nickname: String?) {
        val key = stringPreferencesKey(userId)
        context.ixLocalNicknamesDataStore.edit { preferences ->
            val sanitizedNickname = nickname?.trim().orEmpty()
            if (sanitizedNickname.isEmpty()) {
                preferences.remove(key)
            } else {
                preferences[key] = sanitizedNickname
            }
        }
    }
}
