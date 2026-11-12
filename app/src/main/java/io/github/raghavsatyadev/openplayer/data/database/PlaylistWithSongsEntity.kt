package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.Embedded
import androidx.room3.Relation

data class PlaylistWithSongsEntity(
    @Embedded
    val playlist: PlaylistEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["playlist_id"],
        entity = PlaylistSongEntity::class
    )
    val songs: List<PlaylistSongEntity>
)
