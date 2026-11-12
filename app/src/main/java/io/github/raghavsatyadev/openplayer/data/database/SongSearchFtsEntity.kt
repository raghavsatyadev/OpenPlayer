package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.PrimaryKey

@Fts4(tokenizer = "unicode61")
@Entity(tableName = "songs_fts")
data class SongSearchFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "artist_name")
    val artistName: String
)
