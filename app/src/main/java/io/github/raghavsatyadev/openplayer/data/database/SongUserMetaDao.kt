package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SongUserMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: SongUserMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SongUserMetaEntity>)

    @Query("SELECT * FROM song_user_meta WHERE song_id = :songId")
    suspend fun get(songId: String): SongUserMetaEntity?

    @Query("SELECT * FROM song_user_meta WHERE song_id = :songId")
    fun observe(songId: String): Flow<SongUserMetaEntity?>

    @Query("SELECT * FROM song_user_meta")
    fun observeAll(): Flow<List<SongUserMetaEntity>>

    @Query("SELECT * FROM song_user_meta")
    suspend fun getAllOnce(): List<SongUserMetaEntity>

    @Query("SELECT moods FROM song_user_meta WHERE moods != ''")
    suspend fun getAllMoodColumns(): List<String>

    @Query("SELECT tags FROM song_user_meta WHERE tags != ''")
    suspend fun getAllTagColumns(): List<String>

    @Query("DELETE FROM song_user_meta WHERE song_id = :songId")
    suspend fun delete(songId: String)

    @Query("DELETE FROM song_user_meta")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(items: List<SongUserMetaEntity>) {
        deleteAll()
        insertAll(items)
    }
}
