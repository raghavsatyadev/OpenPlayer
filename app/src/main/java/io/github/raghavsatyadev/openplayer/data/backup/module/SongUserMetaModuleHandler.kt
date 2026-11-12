package io.github.raghavsatyadev.openplayer.data.backup.module

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.raghavsatyadev.openplayer.data.backup.model.BackupSection
import io.github.raghavsatyadev.openplayer.data.database.SongUserMetaCodec
import io.github.raghavsatyadev.openplayer.data.database.SongUserMetaDao
import io.github.raghavsatyadev.openplayer.data.database.SongUserMetaEntity
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMeta
import io.github.raghavsatyadev.openplayer.di.BackupGson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-song rating, moods and tags, as an array of
 * `{songId, rating, moods: [..], tags: [..], updatedAt, song: {title, artist, album, duration}}`.
 *
 * Like bookmarks, entries name their song by MediaStore id, so each carries its library metadata
 * under [SONG] and restore points it at this library's id for that song. A local song that cannot
 * be matched is skipped (a wrong id would put the rating on another song); cloud ids are kept.
 */
@Singleton
class SongUserMetaModuleHandler @Inject constructor(
    private val dao: SongUserMetaDao,
    private val songResolver: BackupSongResolver,
    @BackupGson private val gson: Gson
) : BackupModuleHandler {

    override val section = BackupSection.SONG_USER_META

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val rows = dao.getAllOnce()
        val metadata = songResolver.metadataFor(rows.map { it.songId })
        val array = JsonArray()
        rows.forEach { row ->
            val obj = toJson(row)
            metadata[row.songId]?.let { obj.add(SONG, gson.toJsonTree(it)) }
            array.add(obj)
        }
        gson.toJson(array)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) { dao.getAllOnce().size }

    // Rollback restores this device's own rows, so their ids need no matching.
    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val array = JsonArray()
        dao.getAllOnce().forEach { array.add(toJson(it)) }
        gson.toJson(array)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val entries = JsonParser.parseString(payload).asJsonArray
            .filter { it.isJsonObject }
            .mapNotNull { parseEntry(it.asJsonObject) }
        val library = songResolver.library()

        var skipped = 0
        val resolved = entries.mapNotNull { (row, meta) ->
            val id = if (meta == null) row.songId else library.resolve(row.songId, meta)
            when {
                id != null -> row.copy(songId = id)
                isMediaStoreId(row.songId) -> {
                    skipped++
                    null
                }
                else -> row
            }
        }
        if (skipped > 0) Timber.tag(TAG).i("Song meta restore: $skipped entries skipped, song not found")
        // A song named twice keeps its latest entry.
        dao.replaceAll(resolved.groupBy { it.songId }.map { (_, rows) -> rows.maxBy { it.updatedAt } })
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val rows = JsonParser.parseString(snapshot).asJsonArray
            .filter { it.isJsonObject }
            .mapNotNull { parseEntry(it.asJsonObject)?.first }
        dao.replaceAll(rows)
    }

    private fun toJson(row: SongUserMetaEntity): JsonObject = JsonObject().apply {
        addProperty("songId", row.songId)
        addProperty("rating", row.rating)
        add("moods", stringArray(SongUserMetaCodec.decode(row.moods)))
        add("tags", stringArray(SongUserMetaCodec.decode(row.tags)))
        addProperty("updatedAt", row.updatedAt)
    }

    private fun stringArray(values: List<String>): JsonArray {
        val array = JsonArray()
        values.forEach { array.add(it) }
        return array
    }

    private fun JsonObject.strings(name: String): List<String> =
        get(name)?.takeIf { it.isJsonArray }?.asJsonArray
            ?.mapNotNull { runCatching { it.asString }.getOrNull() }.orEmpty()

    /** One entry and the metadata to match its song with, or null when it names no song. */
    @Suppress("SENSELESS_COMPARISON", "UNNECESSARY_SAFE_CALL")
    private fun parseEntry(obj: JsonObject): Pair<SongUserMetaEntity, BackupSongMetadata?>? {
        val songId = obj.get("songId")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
            .ifEmpty { return null }
        val meta = obj.get(SONG)?.takeIf { it.isJsonObject }
            ?.let { runCatching { gson.fromJson(it, BackupSongMetadata::class.java) }.getOrNull() }
            ?.takeIf { it.title != null && it.artist != null }
            ?.let { it.copy(album = it.album ?: "") }
        val rating = runCatching { obj.get("rating").asInt }.getOrDefault(0)
            .coerceIn(0, SongUserMeta.MAX_RATING)
        val moods = SongUserMetaCodec.normalize(obj.strings("moods"))
        val tags = SongUserMetaCodec.normalize(obj.strings("tags"))
        if (rating == 0 && moods.isEmpty() && tags.isEmpty()) return null
        val updatedAt = runCatching { obj.get("updatedAt").asLong }.getOrDefault(0L)
        return SongUserMetaEntity(
            songId = songId,
            rating = rating,
            moods = SongUserMetaCodec.encode(moods),
            tags = SongUserMetaCodec.encode(tags),
            updatedAt = updatedAt
        ) to meta
    }

    private companion object {
        const val TAG = "SongUserMetaBackup"
        const val SONG = "song"

        /** Local songs are keyed by MediaStore id, a positive number; cloud ids are not. */
        fun isMediaStoreId(songId: String): Boolean = (songId.toLongOrNull() ?: 0L) > 0L
    }
}
