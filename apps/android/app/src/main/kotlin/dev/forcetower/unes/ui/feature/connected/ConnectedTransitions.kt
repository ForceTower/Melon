package dev.forcetower.unes.ui.feature.connected

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import dev.forcetower.unes.ui.navigation.iosPop
import dev.forcetower.unes.ui.navigation.iosPush

// Connected-shell transition specs. Wraps the generic iOS push/pop with a
// tab-switch detector: when the top entry changes between two tab roots
// (e.g. Overview → Schedule), `NavDisplay` would otherwise treat it as a
// horizontal push because its diff just sees "different top key". iOS
// `TabView` swaps tabs instantly (the underlying view hierarchy stays
// alive), so we mirror that with `EnterTransition.None`. Intra-tab pushes
// and pops still get the iOS push/pop keyframes.

// Tab roots are tagged via Nav3 entry `metadata` (see ConnectedScreen's
// entryProvider) so the spec can detect tab-swap purely from
// `Scene.metadata` without dipping into the private `NavEntry.key`.
internal const val TabRootMetadataKey = "connected.tabRoot"
internal val TabRootMetadata: Map<String, Any> = mapOf(TabRootMetadataKey to true)

private val Scene<NavKey>.isTabRoot: Boolean
    get() = metadata[TabRootMetadataKey] == true

private fun AnimatedContentTransitionScope<Scene<NavKey>>.isTabSwitch(): Boolean =
    initialState !== targetState && initialState.isTabRoot && targetState.isTabRoot

private val NoTransition: ContentTransform =
    EnterTransition.None togetherWith ExitTransition.None

internal val connectedPushTransition:
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        if (isTabSwitch()) NoTransition else iosPush()
    }

internal val connectedPopTransition:
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        if (isTabSwitch()) NoTransition else iosPop()
    }

// Predictive back is gesture-driven within the active stack; it never
// crosses tabs, so no need to short-circuit here.
internal val connectedPredictivePopTransition:
    AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform = { _ -> iosPop() }
