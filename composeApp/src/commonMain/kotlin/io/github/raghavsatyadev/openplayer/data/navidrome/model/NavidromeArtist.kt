package io.github.raghavsatyadev.openplayer.data.navidrome.model

import io.github.raghavsatyadev.openplayer.utils.parcel.CommonParcelable
import androidx.compose.runtime.Immutable
import io.github.raghavsatyadev.openplayer.utils.parcel.CommonParcelize

/**
 * Represents an artist from a Navidrome/Subsonic server.
 *
 * Based on the Subsonic API "ArtistID3" entity.
 *
 * @property id The unique identifier of the artist on the server
 * @property name The artist name
 * @property coverArt The cover art ID (used to construct artist image URL)
 * @property albumCount The number of albums by this artist
 * @property artistImageUrl Direct URL to the artist image (optional)
 */
@Immutable
@CommonParcelize
data class NavidromeArtist(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val albumCount: Int = 0,
    val artistImageUrl: String? = null
) : CommonParcelable {
    companion object {
        fun empty() = NavidromeArtist(
            id = "",
            name = "",
            coverArt = null,
            albumCount = 0,
            artistImageUrl = null
        )
    }
}
