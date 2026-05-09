/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.lib.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

class IxRoomMediaAutoDownloadStore(
    private val context: Context,
) {
    private val store = provideDataStore(context)

    fun enabledFlow(roomId: String): Flow<Boolean> {
        val key = booleanPreferencesKey(roomKey(roomId))
        return store.data
            .catch { throwable ->
                Timber.w(throwable, "Failed to read Iltix room media auto download settings")
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[key] ?: false
            }
    }

    suspend fun setEnabled(roomId: String, enabled: Boolean) {
        val key = booleanPreferencesKey(roomKey(roomId))
        store.edit { preferences ->
            preferences[key] = enabled
        }
    }

    private fun roomKey(roomId: String): String {
        return "room_media_auto_download_${roomId.replace(Regex("[^A-Za-z0-9_]"), "_")}"
    }

    private companion object {
        private const val STORE_NAME = "iltix_room_media_auto_download"
        private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Volatile
        private var dataStore: DataStore<Preferences>? = null

        fun provideDataStore(context: Context): DataStore<Preferences> {
            val existing = dataStore
            if (existing != null) return existing
            return synchronized(this) {
                val doubleChecked = dataStore
                if (doubleChecked != null) {
                    doubleChecked
                } else {
                    PreferenceDataStoreFactory.create(scope = storeScope) {
                        context.applicationContext.preferencesDataStoreFile(STORE_NAME)
                    }.also { created ->
                        dataStore = created
                    }
                }
            }
        }
    }
}
