package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

/** A local song that may have no genre; see [GenreFillDao.getGenreFillCandidates]. */
data class GenreFillCandidateRow(
    val id: Long,
    val title: String,
    @ColumnInfo(name = "artist_name") val artistName: String,
    @ColumnInfo(name = "album_name") val albumName: String,
    val duration: Long,
    val genre: String?
)

/** Reads and writes for filling in genres that song files do not carry. */
@Dao
interface GenreFillDao {

    /**
     * Local songs whose genre may be empty and that the user never edited. Same prefilter as
     * MusicDao.getUnknownGenreSongCandidates: callers keep the rows GenreParser.split finds empty.
     */
    @Query("""
        SELECT id, title, artist_name, album_name, duration, genre FROM songs
        WHERE source_type = 0
        AND genre_user_edited = 0
        AND (
            genre IS NULL
            OR TRIM(genre, :noGenreCharacters) = ''
            OR genre LIKE '%unknown%'
            OR instr(genre, char(0)) > 0
        )
    """)
    suspend fun getGenreFillCandidates(noGenreCharacters: String): List<GenreFillCandidateRow>

    /** Never touches a genre the user set. */
    @Query("UPDATE songs SET genre = :genre WHERE id = :songId AND genre_user_edited = 0")
    suspend fun setInferredGenre(songId: Long, genre: String)

    @Transaction
    suspend fun setInferredGenres(genres: Map<Long, String>) {
        genres.forEach { (songId, genre) -> setInferredGenre(songId, genre) }
    }

    @Query("SELECT * FROM genre_lookup_cache WHERE lookup_key IN (:keys)")
    suspend fun getCachedLookups(keys: List<String>): List<GenreLookupCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCachedLookup(entry: GenreLookupCacheEntity)

    @Query("DELETE FROM genre_lookup_cache WHERE genre IS NULL")
    suspend fun clearCachedMisses()
}
