/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.lib.preferences

import de.iltix.lib.R

/**
 * All Iltix feature preferences in a single object.
 * All features default to true (enabled by default in Iltix).
 */
object IxPrefs {
    // ===== General Features =====
    val LOCAL_USERNAMES = IxBoolPref(
        key = "ILTIX_LOCAL_USERNAMES",
        defaultValue = true,
        titleRes = R.string.iltix_local_usernames_title,
        summaryRes = R.string.iltix_local_usernames_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    // ===== Chat Overview Features =====
    /** Legacy bool pref kept for migration; use SPACE_NAV_MODE instead. */
    val SPACE_NAV = IxBoolPref(
        key = "ILTIX_SPACE_NAV",
        defaultValue = true,
        titleRes = R.string.iltix_space_nav_title,
        summaryRes = R.string.iltix_space_nav_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    /** Combined space navigation: "none" | "combined". */
    val SPACE_NAV_MODE = IxListPref(
        key = "ILTIX_SPACE_NAV_MODE",
        defaultValue = "combined",
        titleRes = R.string.iltix_space_nav_title,
        summaryRes = R.string.iltix_space_nav_subtitle,
        authorsChoice = "combined",
        upstreamChoice = "none",
        entries = listOf("Off", "Combined"),
        entryValues = listOf("none", "combined"),
    )

    val NUMBER_BADGE = IxBoolPref(
        key = "ILTIX_NUMBER_BADGE",
        defaultValue = true,
        titleRes = R.string.iltix_number_badge_title,
        summaryRes = R.string.iltix_number_badge_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    val START_BUTTON_IN_TOOLBAR = IxBoolPref(
        key = "ILTIX_START_BUTTON_TOOLBAR",
        defaultValue = true,
        titleRes = R.string.iltix_start_button_toolbar_title,
        summaryRes = R.string.iltix_start_button_toolbar_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    val PIN_FAVORITES = IxBoolPref(
        key = "ILTIX_PIN_FAVORITES",
        defaultValue = true,
        titleRes = R.string.iltix_pin_favorites_title,
        summaryRes = R.string.iltix_pin_favorites_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    // ===== Theme Features =====
    val ILTIX_THEME = IxBoolPref(
        key = "ILTIX_THEME",
        defaultValue = true,
        titleRes = R.string.iltix_theme_toggle_title,
        summaryRes = R.string.iltix_theme_toggle_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    val MATERIAL_YOU_THEME = IxBoolPref(
        key = "ILTIX_MATERIAL_YOU_THEME",
        defaultValue = true,
        titleRes = R.string.iltix_material_you_theme_title,
        summaryRes = R.string.iltix_material_you_theme_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    val ROUNDED_BUBBLES = IxBoolPref(
        key = "ILTIX_ROUNDED_BUBBLES",
        defaultValue = true,
        titleRes = R.string.iltix_rounded_bubbles_title,
        summaryRes = R.string.iltix_rounded_bubbles_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    val CARD_ROOM_ROWS = IxBoolPref(
        key = "ILTIX_CARD_ROOM_ROWS",
        defaultValue = true,
        titleRes = R.string.iltix_card_room_rows_title,
        summaryRes = R.string.iltix_card_room_rows_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    val ILTIX_FONT = IxListPref(
        key = "ILTIX_FONT_FAMILY",
        defaultValue = "noto_sans",
        titleRes = R.string.iltix_font_title,
        summaryRes = R.string.iltix_font_subtitle,
        authorsChoice = "noto_sans",
        upstreamChoice = "system",
        entries = listOf("Noto Sans", "Roboto Condensed", "System"),
        entryValues = listOf("noto_sans", "roboto_condensed", "system"),
    )

    // ===== Chat View Features =====
    val EMOJI_PICKER = IxBoolPref(
        key = "ILTIX_EMOJI_PICKER",
        defaultValue = true,
        titleRes = R.string.iltix_emoji_picker_title,
        summaryRes = R.string.iltix_emoji_picker_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    val POLL_VOTE_VIEWER = IxBoolPref(
        key = "ILTIX_POLL_VOTE_VIEWER",
        defaultValue = true,
        titleRes = R.string.iltix_poll_vote_viewer_title,
        summaryRes = R.string.iltix_poll_vote_viewer_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    val PRIORITY_NOTIFICATION = IxBoolPref(
        key = "ILTIX_PRIORITY_NOTIFICATION",
        defaultValue = true,
        titleRes = R.string.iltix_priority_notification_title,
        summaryRes = R.string.iltix_priority_notification_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    val UNENCRYPTED_TOPBAR_ICON = IxBoolPref(
        key = "ILTIX_UNENCRYPTED_TOPBAR_ICON",
        defaultValue = true,
        titleRes = R.string.iltix_unencrypted_topbar_icon_title,
        summaryRes = R.string.iltix_unencrypted_topbar_icon_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    val MEDIA_AUTO_DOWNLOAD = IxBoolPref(
        key = "ILTIX_MEDIA_AUTO_DOWNLOAD",
        defaultValue = true,
        titleRes = R.string.iltix_media_auto_download_title,
        summaryRes = R.string.iltix_media_auto_download_subtitle,
        authorsChoice = true,
        upstreamChoice = false,
    )

    val MEDIA_AUTO_DOWNLOAD_NETWORK_MODE = IxListPref(
        key = "ILTIX_MEDIA_AUTO_DOWNLOAD_NETWORK_MODE",
        defaultValue = "wifi_and_mobile",
        titleRes = R.string.iltix_media_auto_download_network_title,
        summaryRes = R.string.iltix_media_auto_download_network_subtitle,
        authorsChoice = "wifi_and_mobile",
        upstreamChoice = "wifi_only",
        entries = listOf("WIFI and Mobile", "WIFI only"),
        entryValues = listOf("wifi_and_mobile", "wifi_only"),
    )

    val MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY = IxBoolPref(
        key = "ILTIX_MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY",
        defaultValue = false,
        titleRes = R.string.iltix_media_auto_download_videos_wifi_only_title,
        summaryRes = R.string.iltix_media_auto_download_videos_wifi_only_subtitle,
        authorsChoice = false,
        upstreamChoice = true,
    )

    val MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB = IxIntPref(
        key = "ILTIX_MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB",
        defaultValue = 20,
        titleRes = R.string.iltix_media_auto_download_mobile_video_limit_title,
        summaryRes = R.string.iltix_media_auto_download_mobile_video_limit_subtitle,
        authorsChoice = 20,
        upstreamChoice = 0,
    )

    /**
     * Get all preferences as a list for iteration.
     */
    fun getAllPreferences(): List<IxPref<*>> = listOf(
        LOCAL_USERNAMES,
        SPACE_NAV_MODE,
        NUMBER_BADGE,
        START_BUTTON_IN_TOOLBAR,
        PIN_FAVORITES,
        ILTIX_THEME,
        MATERIAL_YOU_THEME,
        ROUNDED_BUBBLES,
        CARD_ROOM_ROWS,
        ILTIX_FONT,
        EMOJI_PICKER,
        POLL_VOTE_VIEWER,
        PRIORITY_NOTIFICATION,
        UNENCRYPTED_TOPBAR_ICON,
        MEDIA_AUTO_DOWNLOAD,
        MEDIA_AUTO_DOWNLOAD_NETWORK_MODE,
        MEDIA_AUTO_DOWNLOAD_VIDEOS_WIFI_ONLY,
        MEDIA_AUTO_DOWNLOAD_MOBILE_VIDEO_LIMIT_MB,
    )
}
