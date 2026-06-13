/*
 * Copyright (c) 2026 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class IxBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Receiving boot broadcasts is enough to bring the app out of the stopped state.
        Timber.d("IxBootReceiver received action=${intent.action}")
    }
}
