/*
 * Copyright (c) 2025 Iltix Contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package de.iltix.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.libraries.di.SessionScope

@ContributesNode(SessionScope::class)
@AssistedInject
class IxModuleSettingsNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
) : Node(buildContext, plugins = plugins) {
    @Composable
    override fun View(modifier: Modifier) {
        val presenter = remember { IxModuleSettingsPresenter() }
        val state = presenter.present()
        IxModuleSettingsView(
            state = state,
            onBackClick = ::navigateUp,
            modifier = modifier,
        )
    }
}
