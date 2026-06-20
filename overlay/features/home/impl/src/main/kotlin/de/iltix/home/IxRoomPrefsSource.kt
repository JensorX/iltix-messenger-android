/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.home

import android.content.Context
import de.iltix.lib.preferences.IxPreferencesStore
import de.iltix.lib.preferences.IxPrefs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.libraries.di.annotations.ApplicationContext
import kotlinx.coroutines.flow.Flow

/**
 * Provides reactive access to Iltix room-list preferences for use in
 * Molecule Presenters (which cannot access [android.content.Context] via LocalContext).
 */
interface IxRoomPrefsSource {
    fun pinFavoritesFlow(): Flow<Boolean>
    fun showTypingInOverviewFlow(): Flow<Boolean>
}

@Inject
@ContributesBinding(AppScope::class)
class DefaultIxRoomPrefsSource(
    @ApplicationContext private val context: Context,
) : IxRoomPrefsSource {
    private val store by lazy { IxPreferencesStore(context) }

    override fun pinFavoritesFlow(): Flow<Boolean> = store.settingFlow(IxPrefs.PIN_FAVORITES)

    override fun showTypingInOverviewFlow(): Flow<Boolean> = store.settingFlow(IxPrefs.SHOW_TYPING_IN_OVERVIEW)
}
