package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import kotlinx.collections.immutable.ImmutableList
import androidx.compose.runtime.Immutable
import io.github.raghavsatyadev.openplayer.data.model.Genre
import io.github.raghavsatyadev.openplayer.data.model.SortOption

/** The Genres tab's list together with the sort it was ordered by. */
@Immutable
data class LibraryGenresState(
    val genres: ImmutableList<Genre>,
    val sortOption: SortOption
)
