package io.github.raghavsatyadev.openplayer.data.jellyfin.model

import io.github.raghavsatyadev.openplayer.utils.parcel.CommonParcelable
import androidx.compose.runtime.Immutable
import io.github.raghavsatyadev.openplayer.utils.parcel.CommonParcelize

@Immutable
@CommonParcelize
data class JellyfinPlaylist(
    val id: String,
    val name: String,
    val songCount: Int = 0,
    val duration: Long = 0L,
    val created: Long = 0L,
    val changed: Long = 0L
) : CommonParcelable {
    companion object {
        fun empty() = JellyfinPlaylist(
            id = "",
            name = "",
            songCount = 0,
            duration = 0L,
            created = 0L,
            changed = 0L
        )
    }
}
