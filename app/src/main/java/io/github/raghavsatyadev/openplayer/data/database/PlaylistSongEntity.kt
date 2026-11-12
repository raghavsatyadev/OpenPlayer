package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlist_id", "sort_order"],
    indices = [
        Index(value = ["playlist_id", "sort_order"]),
        Index(value = ["song_id"])
    ]
)
data class PlaylistSongEntity(
    @ColumnInfo(name = "playlist_id")
    val playlistId: String,
    @ColumnInfo(name = "song_id")
    val songId: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)
