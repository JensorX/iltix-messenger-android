/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.nowbar

/**
 * Generic source categories for additive Now Bar entries.
 */
enum class NowBarSource {
    Call,
    Media,
    FavoriteChat,
}

/**
 * Coarse-grained priority for ranking and throttling.
 */
enum class NowBarPriority {
    Low,
    Default,
    High,
    Critical,
}

/**
 * Shared payload envelope used by all providers.
 */
data class NowBarEntry(
    val id: String,
    val source: NowBarSource,
    val title: String,
    val text: String,
    val priority: NowBarPriority = NowBarPriority.Default,
    val isOngoing: Boolean = true,
)
