package io.github.raghavsatyadev.openplayer.data.jellyfin.model

import io.github.raghavsatyadev.openplayer.utils.parcel.CommonParcelable
import androidx.compose.runtime.Immutable
import io.github.raghavsatyadev.openplayer.utils.parcel.CommonParcelize

@Immutable
@CommonParcelize
data class JellyfinAlbum(
    val id: String,
    val name: String,
    val artist: String,
    val artistId: String? = null,
    val songCount: Int = 0,
    val duration: Long = 0L,
    val year: Int = 0,
    val genre: String? = null
) : CommonParcelable {
    companion object {
        fun empty() = JellyfinAlbum(
            id = "",
            name = "",
            artist = "",
            artistId = null,
            songCount = 0,
            duration = 0L,
            year = 0,
            genre = null
        )
    }
}
