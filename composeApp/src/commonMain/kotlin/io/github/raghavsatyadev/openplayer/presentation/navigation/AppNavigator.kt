package io.github.raghavsatyadev.openplayer.presentation.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

/**
 * App-wide navigation on top of a Navigation 3 back stack. The stack always starts with the
 * launch tab; another main tab sits right above it, and detail screens stack on top of the tab.
 * Each main tab keeps its own detail stack while another tab is shown, like Nav2's
 * saveState/restoreState did.
 */
@Stable
class AppNavigator(val backStack: NavBackStack<NavKey>) {

    private val savedTabStacks = mutableMapOf<Screen, List<NavKey>>()

    val current: Screen? get() = backStack.lastOrNull() as? Screen

    val previous: Screen? get() = backStack.getOrNull(backStack.size - 2) as? Screen

    private val start: Screen? get() = backStack.firstOrNull() as? Screen

    /** The main tab whose stack is showing: the topmost main-tab entry. */
    val currentTab: Screen? get() = backStack.lastOrNull { (it as? Screen)?.mainTabIndex != null } as? Screen

    /** Pushes [route] unless the same screen with the same arguments is already on top. */
    fun navigateSafely(route: Screen): Boolean {
        if (backStack.lastOrNull() == route) return false
        backStack.add(route)
        return true
    }

    /**
     * Pushes [route] after removing the topmost [patternToPop] entry and everything above it, so
     * jumping from one album to another doesn't pile up album screens. The root is never removed.
     */
    fun navigateSafelyReplacing(route: Screen, patternToPop: KClass<out Screen>): Boolean {
        val index = backStack.indexOfLast { patternToPop.isInstance(it) }
        if (index > 0) removeFrom(index)
        backStack.add(route)
        return true
    }

    /** Pops the top screen. Returns false at the root, where the system should handle back. */
    fun popBackStack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    /** Pops back to the topmost [screen] entry (also removing it when [inclusive]). */
    fun popBackStack(screen: KClass<out Screen>, inclusive: Boolean): Boolean {
        val index = backStack.indexOfLast { screen.isInstance(it) }
        if (index < 0) return false
        val from = if (inclusive) index else index + 1
        if (from >= backStack.size) return false
        removeFrom(from.coerceAtLeast(1))
        return true
    }

    fun navigateUp(): Boolean = popBackStack()

    /** Switches to main tab [tab], restoring the detail screens it had open. */
    fun navigateToTopLevelSafely(tab: Screen): Boolean = switchTab(tab, restore = true)

    /** Switches to main tab [tab] and drops the detail screens it had open. */
    fun navigateToTabRoot(tab: Screen): Boolean = switchTab(tab, restore = false)

    /** Replaces the whole stack with [root]; used once the saved launch tab is known. */
    fun resetRoot(root: Screen) {
        savedTabStacks.clear()
        replaceAll(listOf(root))
    }

    private fun switchTab(tab: Screen, restore: Boolean): Boolean {
        val root = start ?: return false
        val showing = currentTab
        if (showing != null) {
            val showingIndex = backStack.indexOfLast { it == showing }
            savedTabStacks[showing] = backStack.subList(showingIndex + 1, backStack.size).toList()
        }
        if (!restore) savedTabStacks.remove(tab)
        val next = buildList {
            add(root)
            if (tab != root) add(tab)
            addAll(savedTabStacks[tab].orEmpty())
        }
        if (next == backStack.toList()) return false
        replaceAll(next)
        return true
    }

    private fun removeFrom(index: Int) {
        while (backStack.size > index) backStack.removeAt(backStack.lastIndex)
    }

    private fun replaceAll(keys: List<NavKey>) {
        // Keep the common prefix so entries that stay keep their state holders.
        var keep = 0
        while (keep < keys.size && keep < backStack.size && backStack[keep] == keys[keep]) keep++
        removeFrom(keep)
        backStack.addAll(keys.subList(keep, keys.size))
    }
}
