package io.github.raghavsatyadev.openplayer.data.navidrome.model

import io.github.raghavsatyadev.openplayer.utils.parcel.CommonParcelable
import androidx.compose.runtime.Immutable
import io.github.raghavsatyadev.openplayer.utils.parcel.CommonParcelize

/**
 * Represents a music folder (library) from a Navidrome/Subsonic server.
 *
 * Based on the Subsonic API "MusicFolder" entity.
 *
 * @property id The unique identifier of the music folder
 * @property name The display name of the music folder
 */
@Immutable
@CommonParcelize
data class NavidromeMusicFolder(
    val id: String,
    val name: String
) : CommonParcelable {
    companion object {
        fun empty() = NavidromeMusicFolder(
            id = "",
            name = ""
        )
    }
}
