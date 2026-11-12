package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One online genre lookup, keyed by artist and album (or artist and title for songs with no
 * album). [genre] is null when the lookup ran and found nothing; [checkedAt] says when, so a miss
 * is retried only after a while. Rescans clear song genres that came from here, and this table
 * puts them back without asking the network again.
 */
@Entity(tableName = "genre_lookup_cache")
data class GenreLookupCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "lookup_key")
    val lookupKey: String,

    @ColumnInfo(name = "genre")
    val genre: String?,

    @ColumnInfo(name = "checked_at")
    val checkedAt: Long
)
