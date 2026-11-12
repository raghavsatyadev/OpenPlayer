package io.github.raghavsatyadev.openplayer.data.repository

import io.github.raghavsatyadev.openplayer.data.database.SongUserMetaCodec
import io.github.raghavsatyadev.openplayer.data.database.SongUserMetaDao
import io.github.raghavsatyadev.openplayer.data.database.SongUserMetaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A song's rating (0 = unrated, up to [MAX_RATING]), moods and tags. */
data class SongUserMeta(
    val songId: String,
    val rating: Int = 0,
    val moods: List<String> = emptyList(),
    val tags: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = rating == 0 && moods.isEmpty() && tags.isEmpty()

    companion object {
        const val MAX_RATING = 5
    }
}

/** Per-song rating, moods and tags (Namida's track stats), stored in `song_user_meta`. */
class SongUserMetaRepository(
    private val dao: SongUserMetaDao
) {
    fun observe(songId: String): Flow<SongUserMeta?> = dao.observe(songId).map { it?.toModel() }

    suspend fun get(songId: String): SongUserMeta? = dao.get(songId)?.toModel()

    /** Every song's meta by song id, kept up to date; for evaluating rule playlists. */
    fun observeAll(): Flow<Map<String, SongUserMeta>> = dao.observeAll().map { rows ->
        rows.associate { it.songId to it.toModel() }
    }

    /** Rating by song id, for lists that show it; unrated songs are left out. */
    fun observeRatings(): Flow<Map<String, Int>> = dao.observeAll().map { rows ->
        rows.filter { it.rating > 0 }.associate { it.songId to it.rating }
    }

    /** Saves [meta]; a song with nothing set has its row removed. */
    suspend fun save(meta: SongUserMeta, now: Long = System.currentTimeMillis()) {
        val clean = meta.copy(
            rating = meta.rating.coerceIn(0, SongUserMeta.MAX_RATING),
            moods = SongUserMetaCodec.normalize(meta.moods),
            tags = SongUserMetaCodec.normalize(meta.tags)
        )
        if (clean.isEmpty) {
            dao.delete(clean.songId)
        } else {
            dao.upsert(
                SongUserMetaEntity(
                    songId = clean.songId,
                    rating = clean.rating,
                    moods = SongUserMetaCodec.encode(clean.moods),
                    tags = SongUserMetaCodec.encode(clean.tags),
                    updatedAt = now
                )
            )
        }
    }

    /** Every mood used on any song, most used first, then alphabetical. */
    suspend fun moodSuggestions(): List<String> = rank(dao.getAllMoodColumns())

    /** Every tag used on any song, most used first, then alphabetical. */
    suspend fun tagSuggestions(): List<String> = rank(dao.getAllTagColumns())

    private fun rank(columns: List<String>): List<String> {
        val counts = LinkedHashMap<String, Pair<String, Int>>()
        columns.forEach { raw ->
            SongUserMetaCodec.decode(raw).forEach { value ->
                val key = value.lowercase()
                val current = counts[key]
                counts[key] = (current?.first ?: value) to ((current?.second ?: 0) + 1)
            }
        }
        return counts.values
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase() })
            .map { it.first }
    }

    private fun SongUserMetaEntity.toModel() = SongUserMeta(
        songId = songId,
        rating = rating.coerceIn(0, SongUserMeta.MAX_RATING),
        moods = SongUserMetaCodec.decode(moods),
        tags = SongUserMetaCodec.decode(tags)
    )
}
