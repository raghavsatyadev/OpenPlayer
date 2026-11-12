package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * A named snapshot of the play queue (Namida's queue history). [songIds] is a JSON array of song
 * ids (see [SavedQueueCodec]); [currentIndex] and [positionMs] say where playback was.
 */
@Entity(tableName = "saved_queues")
data class SavedQueueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "song_ids")
    val songIds: String,

    @ColumnInfo(name = "current_index")
    val currentIndex: Int,

    @ColumnInfo(name = "position_ms")
    val positionMs: Long
)
