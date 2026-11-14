package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import io.github.raghavsatyadev.openplayer.data.model.LibraryTabId

/**
 * A Locate request for one Library list. [index] is the current song's position in [tab]'s own
 * order, so only that tab's list scrolls; a neighbouring pager page keeps its position.
 */
data class LocateScrollRequest(
    val tab: LibraryTabId,
    val index: Int
)
