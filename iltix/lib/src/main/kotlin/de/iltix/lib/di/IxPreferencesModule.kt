/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.lib.di

import android.content.Context
import de.iltix.lib.preferences.IxPreferencesStore

/**
 * Provides a singleton instance of IxPreferencesStore.
 *
 * Note: Most Iltix Composables create IxPreferencesStore(context) directly
 * via LocalContext. This provider is available for non-Composable contexts
 * where DI is not used.
 */
class IxPreferencesProvider(private val context: Context) {
    private val _store by lazy { IxPreferencesStore(context) }

    fun getStore(): IxPreferencesStore = _store
}
