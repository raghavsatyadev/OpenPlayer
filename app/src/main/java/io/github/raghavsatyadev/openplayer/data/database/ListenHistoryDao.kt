package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import io.github.raghavsatyadev.openplayer.data.history.recommend.TimedListen
import kotlinx.coroutines.flow.Flow

@Dao
interface ListenHistoryDao {

    @Insert
    suspend fun insert(listen: ListenHistoryEntity): Long

    @Insert
    suspend fun insertAll(listens: List<ListenHistoryEntity>)

    @Query("SELECT COUNT(*) FROM listen_history")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM listen_history WHERE song_id = :songId")
    suspend fun countForSong(songId: String): Int

    /** Newest first; estimated rows have no real time, so they are left out. */
    @Query(
        """
        SELECT * FROM listen_history
        WHERE source != ${ListenSource.ESTIMATED_FROM_PLAY_COUNT}
        ORDER BY ended_at DESC, id DESC
        LIMIT :limit
        """
    )
    fun recentListensFlow(limit: Int): Flow<List<ListenHistoryEntity>>

    @Query(
        """
        SELECT * FROM listen_history
        WHERE source != ${ListenSource.ESTIMATED_FROM_PLAY_COUNT}
          AND ended_at >= :fromInclusive AND ended_at < :toExclusive
        ORDER BY ended_at ASC, id ASC
        """
    )
    suspend fun listensBetween(fromInclusive: Long, toExclusive: Long): List<ListenHistoryEntity>

    /** Every listen with a real time, oldest first, for the history picks. */
    @Query(
        """
        SELECT song_id, ended_at FROM listen_history
        WHERE source != ${ListenSource.ESTIMATED_FROM_PLAY_COUNT}
        ORDER BY ended_at ASC, id ASC
        """
    )
    suspend fun timedListens(): List<TimedListen>

    /** Changes whenever a listen is added or removed. */
    @Query("SELECT COUNT(*) FROM listen_history")
    fun countFlow(): Flow<Int>

    /** Listens per song since [sinceInclusive] (0 = all time, estimated rows included). */
    @Query(
        """
        SELECT song_id, COUNT(*) AS listen_count, MAX(ended_at) AS last_listened_at
        FROM listen_history
        WHERE :sinceInclusive <= 0
           OR (ended_at >= :sinceInclusive AND source != ${ListenSource.ESTIMATED_FROM_PLAY_COUNT})
        GROUP BY song_id
        ORDER BY listen_count DESC, last_listened_at DESC
        """
    )
    suspend fun listenCounts(sinceInclusive: Long = 0L): List<SongListenCount>

    /** Start times of every listen with a real time that started in the range, for import dedupe. */
    @Query(
        """
        SELECT song_id, started_at FROM listen_history
        WHERE source != ${ListenSource.ESTIMATED_FROM_PLAY_COUNT}
          AND started_at >= :fromInclusive AND started_at <= :toInclusive
        """
    )
    suspend fun startsBetween(fromInclusive: Long, toInclusive: Long): List<ListenStartRow>

    @Query("SELECT id, duration FROM songs")
    suspend fun getSongDurations(): List<SongDurationRow>

    @Query("DELETE FROM listen_history WHERE song_id = :songId")
    suspend fun deleteForSong(songId: String)

    @Query("DELETE FROM listen_history")
    suspend fun clear()

    @Query("SELECT * FROM listen_history ORDER BY song_id, started_at, id")
    suspend fun getAllOnce(): List<ListenHistoryEntity>

    @Transaction
    suspend fun replaceAll(listens: List<ListenHistoryEntity>) {
        clear()
        listens.chunked(1000).forEach { insertAll(it) }
    }
}
