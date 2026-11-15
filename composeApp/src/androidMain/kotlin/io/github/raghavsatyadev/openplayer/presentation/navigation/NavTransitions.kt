package io.github.raghavsatyadev.openplayer.presentation.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

/** Entry metadata key holding a main tab's index, set by [screen]. */
internal const val MAIN_TAB_INDEX = "openplayer.mainTabIndex"

private const val BOTTOM_NAV_TRANSITION_DURATION = 380

private val BottomNavEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private val MAIN_ROOT_TRANSITION_SPEC =
    tween<IntOffset>(durationMillis = BOTTOM_NAV_TRANSITION_DURATION, easing = BottomNavEasing)

private val MAIN_ROOT_FADE_SPEC =
    tween<Float>(durationMillis = BOTTOM_NAV_TRANSITION_DURATION / 2, easing = BottomNavEasing)

private fun Scene<NavKey>.tabIndex(): Int? =
    entries.lastOrNull()?.metadata?.get(MAIN_TAB_INDEX) as? Int

/**
 * Switching between main tabs slides toward the tab's side (+1 forward, -1 backward);
 * null when either side isn't a main tab or both are the same tab.
 */
private fun tabDirection(from: Scene<NavKey>, to: Scene<NavKey>): Int? {
    val fromIndex = from.tabIndex() ?: return null
    val toIndex = to.tabIndex() ?: return null
    if (fromIndex == toIndex) return null
    return if (toIndex > fromIndex) 1 else -1
}

private fun tabSlide(direction: Int): ContentTransform =
    slideInHorizontally(MAIN_ROOT_TRANSITION_SPEC) { (it * 0.5f).toInt() * direction } +
        fadeIn(MAIN_ROOT_FADE_SPEC) togetherWith
        slideOutHorizontally(MAIN_ROOT_TRANSITION_SPEC) { -(it * 0.5f).toInt() * direction } +
        fadeOut(MAIN_ROOT_FADE_SPEC)

internal fun pushTransform(from: Scene<NavKey>, to: Scene<NavKey>): ContentTransform =
    tabDirection(from, to)?.let(::tabSlide) ?: (enterTransition() togetherWith exitTransition())

internal fun popTransform(from: Scene<NavKey>, to: Scene<NavKey>): ContentTransform =
    tabDirection(from, to)?.let(::tabSlide) ?: (popEnterTransition() togetherWith popExitTransition())
