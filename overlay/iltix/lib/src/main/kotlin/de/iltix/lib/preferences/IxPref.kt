/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.lib.preferences

import androidx.annotation.StringRes

/**
 * Base sealed interface for Iltix preference types.
 */
sealed interface IxPref<T> {
    val key: String
    val defaultValue: T
    val titleRes: Int?
    val summaryRes: Int?
    val authorsChoice: T
    val upstreamChoice: T
}

/**
 * Boolean preference for Iltix features.
 */
data class IxBoolPref(
    override val key: String,
    override val defaultValue: Boolean,
    @StringRes override val titleRes: Int? = null,
    @StringRes override val summaryRes: Int? = null,
    override val authorsChoice: Boolean = defaultValue,
    override val upstreamChoice: Boolean = false,
) : IxPref<Boolean>

/**
 * Integer preference for Iltix features.
 */
data class IxIntPref(
    override val key: String,
    override val defaultValue: Int,
    @StringRes override val titleRes: Int? = null,
    @StringRes override val summaryRes: Int? = null,
    override val authorsChoice: Int = defaultValue,
    override val upstreamChoice: Int = 0,
) : IxPref<Int>

/**
 * String list preference for Iltix features.
 */
data class IxListPref(
    override val key: String,
    override val defaultValue: String,
    @StringRes override val titleRes: Int? = null,
    @StringRes override val summaryRes: Int? = null,
    override val authorsChoice: String = defaultValue,
    override val upstreamChoice: String = "",
    val entries: List<String> = emptyList(),
    val entryValues: List<String> = emptyList(),
) : IxPref<String>

/**
 * Color preference for Iltix features.
 */
data class IxColorPref(
    override val key: String,
    override val defaultValue: Int,
    @StringRes override val titleRes: Int? = null,
    @StringRes override val summaryRes: Int? = null,
    override val authorsChoice: Int = defaultValue,
    override val upstreamChoice: Int = 0,
) : IxPref<Int>
