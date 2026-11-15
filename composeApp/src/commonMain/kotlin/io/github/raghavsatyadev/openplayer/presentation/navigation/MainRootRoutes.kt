package io.github.raghavsatyadev.openplayer.presentation.navigation

import androidx.navigation3.runtime.NavKey

internal fun isMainRootRoute(key: NavKey?): Boolean = mainRootRouteIndex(key) != null

internal fun mainRootRouteIndex(key: NavKey?): Int? = (key as? Screen)?.mainTabIndex
