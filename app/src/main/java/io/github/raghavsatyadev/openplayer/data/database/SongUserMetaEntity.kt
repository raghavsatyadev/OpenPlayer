package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * What the user says about one song (Namida's rating, moods and tags). [rating] is 0 (unrated) to
 * 5 stars; [moods] and [tags] are JSON arrays of strings (see [SongUserMetaCodec]).
 */
@Entity(tableName = "song_user_meta")
data class SongUserMetaEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_id")
    val songId: String,

    @ColumnInfo(name = "rating", defaultValue = "0")
    val rating: Int = 0,

    @ColumnInfo(name = "moods", defaultValue = "")
    val moods: String = "",

    @ColumnInfo(name = "tags", defaultValue = "")
    val tags: String = "",

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
