package io.github.raghavsatyadev.openplayer.data.repository

import io.github.raghavsatyadev.openplayer.data.database.SavedQueueCodec
import io.github.raghavsatyadev.openplayer.data.database.SavedQueueDao
import io.github.raghavsatyadev.openplayer.data.database.SavedQueueEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** A named queue snapshot: its songs (by id), and the song and position playback was at. */
data class SavedQueue(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songIds: List<String>,
    val currentIndex: Int,
    val positionMs: Long
)

/** Saved named queues (Namida's queue history), stored in `saved_queues`. */
@Singleton
class SavedQueueRepository @Inject constructor(
    private val dao: SavedQueueDao
) {
    fun observeAll(): Flow<List<SavedQueue>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    suspend fun get(id: Long): SavedQueue? = dao.getById(id)?.toModel()

    /** Saves a snapshot and returns its id. An empty queue is not saved (returns 0). */
    suspend fun save(
        name: String,
        songIds: List<String>,
        currentIndex: Int,
        positionMs: Long,
        now: Long = System.currentTimeMillis()
    ): Long {
        if (songIds.isEmpty()) return 0L
        return dao.insert(
            SavedQueueEntity(
                name = name.trim().ifEmpty { now.toString() },
                createdAt = now,
                songIds = SavedQueueCodec.encode(songIds),
                currentIndex = currentIndex.coerceIn(0, songIds.lastIndex),
                positionMs = positionMs.coerceAtLeast(0L)
            )
        )
    }

    suspend fun rename(id: Long, name: String) {
        name.trim().takeIf { it.isNotEmpty() }?.let { dao.rename(id, it) }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    private fun SavedQueueEntity.toModel() = SavedQueue(
        id = id,
        name = name,
        createdAt = createdAt,
        songIds = SavedQueueCodec.decode(songIds),
        currentIndex = currentIndex,
        positionMs = positionMs
    )
}
