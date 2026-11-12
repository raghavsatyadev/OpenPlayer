package io.github.raghavsatyadev.openplayer.data.backup.module

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.raghavsatyadev.openplayer.data.backup.model.BackupSection
import io.github.raghavsatyadev.openplayer.data.database.SavedQueueCodec
import io.github.raghavsatyadev.openplayer.data.database.SavedQueueDao
import io.github.raghavsatyadev.openplayer.data.database.SavedQueueEntity
import io.github.raghavsatyadev.openplayer.di.BackupGson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saved queues, as an array of
 * `{name, createdAt, songIds: [..], currentIndex, positionMs, songs: [metadata | null, ..]}`
 * where `songs` runs parallel to `songIds` and lets restore match local songs on another device.
 *
 * On restore a local song that cannot be matched is dropped from its queue (the saved position
 * moves with the songs that are kept); a queue left with no songs is skipped.
 */
@Singleton
class SavedQueuesModuleHandler @Inject constructor(
    private val dao: SavedQueueDao,
    private val songResolver: BackupSongResolver,
    @BackupGson private val gson: Gson
) : BackupModuleHandler {

    override val section = BackupSection.SAVED_QUEUES

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val rows = dao.getAllOnce()
        val metadata = songResolver.metadataFor(rows.flatMap { SavedQueueCodec.decode(it.songIds) })
        val array = JsonArray()
        rows.forEach { row ->
            val ids = SavedQueueCodec.decode(row.songIds)
            val obj = toJson(row, ids)
            val songs = JsonArray()
            ids.forEach { id -> songs.add(metadata[id]?.let { gson.toJsonTree(it) } ?: JsonNull.INSTANCE) }
            obj.add("songs", songs)
            array.add(obj)
        }
        gson.toJson(array)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) { dao.getAllOnce().size }

    // Rollback restores this device's own rows, so their ids need no matching.
    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val array = JsonArray()
        dao.getAllOnce().forEach { array.add(toJson(it, SavedQueueCodec.decode(it.songIds))) }
        gson.toJson(array)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val library = songResolver.library()
        var dropped = 0
        val queues = JsonParser.parseString(payload).asJsonArray
            .filter { it.isJsonObject }
            .mapNotNull { parseQueue(it.asJsonObject, library) { dropped++ } }
        if (dropped > 0) Timber.tag(TAG).i("Saved queue restore: $dropped songs dropped, not found")
        dao.replaceAll(queues)
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val queues = JsonParser.parseString(snapshot).asJsonArray
            .filter { it.isJsonObject }
            .mapNotNull { parseQueue(it.asJsonObject, library = null) {} }
        dao.replaceAll(queues)
    }

    private fun toJson(row: SavedQueueEntity, ids: List<String>): JsonObject = JsonObject().apply {
        addProperty("name", row.name)
        addProperty("createdAt", row.createdAt)
        val idArray = JsonArray()
        ids.forEach { idArray.add(it) }
        add("songIds", idArray)
        addProperty("currentIndex", row.currentIndex)
        addProperty("positionMs", row.positionMs)
    }

    /** One queue entity, or null when it has no songs left. A null [library] keeps ids as they are. */
    @Suppress("SENSELESS_COMPARISON", "UNNECESSARY_SAFE_CALL")
    private fun parseQueue(
        obj: JsonObject,
        library: BackupSongResolver.Library?,
        onDropped: () -> Unit
    ): SavedQueueEntity? {
        val ids = obj.get("songIds")?.takeIf { it.isJsonArray }?.asJsonArray
            ?.mapNotNull { runCatching { it.asString }.getOrNull() }.orEmpty()
        val metas = obj.get("songs")?.takeIf { it.isJsonArray }?.asJsonArray
            ?.map { el ->
                el.takeIf { it.isJsonObject }
                    ?.let { runCatching { gson.fromJson(it, BackupSongMetadata::class.java) }.getOrNull() }
                    ?.takeIf { it.title != null && it.artist != null }
                    ?.let { it.copy(album = it.album ?: "") }
            }.orEmpty()
        val originalIndex = runCatching { obj.get("currentIndex").asInt }.getOrDefault(0)

        val kept = ArrayList<String>(ids.size)
        var keptBeforeCurrent = 0
        ids.forEachIndexed { i, id ->
            val meta = metas.getOrNull(i)
            val resolved = when {
                library == null -> id
                meta != null -> library.resolve(id, meta)
                (id.toLongOrNull() ?: 0L) > 0L -> library.resolve(id, null)
                else -> id
            }
            if (resolved == null) {
                onDropped()
            } else {
                if (i < originalIndex) keptBeforeCurrent++
                kept += resolved
            }
        }
        if (kept.isEmpty()) return null
        return SavedQueueEntity(
            name = obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty().ifBlank { "Queue" },
            createdAt = runCatching { obj.get("createdAt").asLong }.getOrDefault(0L),
            songIds = SavedQueueCodec.encode(kept),
            currentIndex = keptBeforeCurrent.coerceIn(0, kept.lastIndex),
            positionMs = runCatching { obj.get("positionMs").asLong }.getOrDefault(0L).coerceAtLeast(0L)
        )
    }

    private companion object {
        const val TAG = "SavedQueuesBackup"
    }
}
