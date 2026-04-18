/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.lib.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Stores and manages Iltix preferences using Android DataStore.
 */
class IxPreferencesStore(private val context: Context) {
    private val store = provideDataStore(context)

    fun <T> settingFlow(pref: IxPref<T>): Flow<T> {
        return store.data
            .catch { throwable ->
                Timber.w(throwable, "Failed to read Iltix preferences, falling back to defaults")
                emit(emptyPreferences())
            }
            .map { preferences ->
                when (pref) {
                    is IxBoolPref -> {
                        val key = booleanPreferencesKey(pref.key)
                        preferences[key] ?: pref.defaultValue
                    }
                    is IxIntPref -> {
                        val key = intPreferencesKey(pref.key)
                        preferences[key] ?: pref.defaultValue
                    }
                    is IxListPref -> {
                        val key = stringPreferencesKey(pref.key)
                        preferences[key] ?: pref.defaultValue
                    }
                    is IxColorPref -> {
                        val key = intPreferencesKey(pref.key)
                        preferences[key] ?: pref.defaultValue
                    }
                } as T
            }
    }

    suspend fun <T> setSetting(pref: IxPref<T>, value: T) {
        store.edit { preferences ->
            when (pref) {
                is IxBoolPref -> {
                    val key = booleanPreferencesKey(pref.key)
                    preferences[key] = value as Boolean
                }
                is IxIntPref -> {
                    val key = intPreferencesKey(pref.key)
                    preferences[key] = value as Int
                }
                is IxListPref -> {
                    val key = stringPreferencesKey(pref.key)
                    preferences[key] = value as String
                }
                is IxColorPref -> {
                    val key = intPreferencesKey(pref.key)
                    preferences[key] = value as Int
                }
            }
        }
    }

    /**
     * Combine multiple preferences into a single flow for reactive updates.
     */
    fun <R> combinedSettingFlow(block: suspend (IxPrefLookup) -> R): Flow<R> {
        return store.data
            .catch { throwable ->
                Timber.w(throwable, "Failed to read combined Iltix preferences, falling back to defaults")
                emit(emptyPreferences())
            }
            .map { preferences ->
                val lookup = IxPrefLookup(preferences)
                block(lookup)
            }
    }

    suspend fun reset() {
        store.edit { it.clear() }
    }

    private companion object {
        private const val STORE_NAME = "iltix_preferences"
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
                    val appContext = context.applicationContext
                    PreferenceDataStoreFactory.create(
                        scope = storeScope,
                        migrations = listOf(SharedPreferencesMigration(appContext, "iltix_shared_prefs")),
                    ) {
                        appContext.preferencesDataStoreFile(STORE_NAME)
                    }.also { created ->
                        dataStore = created
                    }
                }
            }
        }
    }
}

/**
 * Helper class to safely look up preference values from DataStore.
 */
class IxPrefLookup(private val preferences: Preferences) {
    fun <T> lookup(pref: IxPref<T>): T {
        return when (pref) {
            is IxBoolPref -> {
                val key = booleanPreferencesKey(pref.key)
                preferences[key] ?: pref.defaultValue
            }
            is IxIntPref -> {
                val key = intPreferencesKey(pref.key)
                preferences[key] ?: pref.defaultValue
            }
            is IxListPref -> {
                val key = stringPreferencesKey(pref.key)
                preferences[key] ?: pref.defaultValue
            }
            is IxColorPref -> {
                val key = intPreferencesKey(pref.key)
                preferences[key] ?: pref.defaultValue
            }
        } as T
    }
}
