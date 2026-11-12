package io.github.raghavsatyadev.openplayer.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One counted listen of a song (Namida-style history). A play becomes a row only once it meets
 * the listen rule, so these rows are what recommendations and "most played" read.
 */
@Entity(
    tableName = "listen_history",
    indices = [
        Index(value = ["song_id"]),
        Index(value = ["ended_at"])
    ]
)
data class ListenHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "song_id")
    val songId: String,

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    /** When the listen ended; the timestamp history is ordered by. */
    @ColumnInfo(name = "ended_at")
    val endedAt: Long,

    @ColumnInfo(name = "listened_ms")
    val listenedMs: Long,

    @ColumnInfo(name = "track_duration_ms")
    val trackDurationMs: Long,

    /** One of [ListenSource]. */
    @ColumnInfo(name = "source")
    val source: Int = ListenSource.LIVE
)

object ListenSource {
    /** Recorded while playing. */
    const val LIVE = 0

    /** Imported from the older per-play stats file (real times). */
    const val IMPORTED_PLAY_EVENT = 1

    /**
     * Carried over from the old per-song play count, for plays older than the stats file.
     * The time is only a placeholder (the song's last play), so time-based features skip these.
     */
    const val ESTIMATED_FROM_PLAY_COUNT = 2

    /** Imported from a Last.fm CSV export (real times). */
    const val IMPORTED_LASTFM = 3

    /** Imported from a Spotify streaming history export (real times). */
    const val IMPORTED_SPOTIFY = 4

    /** Imported from a ListenBrainz export (real times). */
    const val IMPORTED_LISTENBRAINZ = 5

    /** Imported from a YouTube Music watch history (Google Takeout); start times, no play length. */
    const val IMPORTED_YOUTUBE_MUSIC = 6
}

/** Start time of a stored listen, to spot plays an import already has. */
data class ListenStartRow(
    @ColumnInfo(name = "song_id") val songId: String,
    @ColumnInfo(name = "started_at") val startedAt: Long
)

/** Listens of one song, for ranking. */
data class SongListenCount(
    @ColumnInfo(name = "song_id") val songId: String,
    @ColumnInfo(name = "listen_count") val listenCount: Int,
    @ColumnInfo(name = "last_listened_at") val lastListenedAt: Long
)

/** Song length from the library, used to apply the listen rule to imported plays. */
data class SongDurationRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "duration") val duration: Long
)
