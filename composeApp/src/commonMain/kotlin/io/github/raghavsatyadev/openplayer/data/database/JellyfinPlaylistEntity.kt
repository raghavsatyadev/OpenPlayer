package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "jellyfin_playlists")
data class JellyfinPlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "song_count") val songCount: Int,
    val duration: Long,
    @ColumnInfo(name = "last_sync_time") val lastSyncTime: Long
)
