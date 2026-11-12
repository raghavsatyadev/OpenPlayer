package io.github.raghavsatyadev.openplayer.data.jellyfin.model

import io.github.raghavsatyadev.openplayer.utils.parcel.CommonParcelable
import androidx.compose.runtime.Immutable
import io.github.raghavsatyadev.openplayer.utils.parcel.CommonParcelize

@Immutable
@CommonParcelize
data class JellyfinArtist(
    val id: String,
    val name: String,
    val albumCount: Int = 0
) : CommonParcelable {
    companion object {
        fun empty() = JellyfinArtist(
            id = "",
            name = "",
            albumCount = 0
        )
    }
}
