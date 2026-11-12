package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedQueueDao {
    @Insert
    suspend fun insert(queue: SavedQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(queues: List<SavedQueueEntity>)

    @Query("SELECT * FROM saved_queues ORDER BY created_at DESC, id DESC")
    fun observeAll(): Flow<List<SavedQueueEntity>>

    @Query("SELECT * FROM saved_queues ORDER BY created_at DESC, id DESC")
    suspend fun getAllOnce(): List<SavedQueueEntity>

    @Query("SELECT * FROM saved_queues WHERE id = :id")
    suspend fun getById(id: Long): SavedQueueEntity?

    @Query("UPDATE saved_queues SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM saved_queues WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM saved_queues")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(queues: List<SavedQueueEntity>) {
        deleteAll()
        insertAll(queues)
    }
}
