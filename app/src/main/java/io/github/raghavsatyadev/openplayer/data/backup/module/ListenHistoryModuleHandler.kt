package io.github.raghavsatyadev.openplayer.data.backup.module

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.raghavsatyadev.openplayer.data.backup.model.BackupSection
import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryDao
import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryEntity
import io.github.raghavsatyadev.openplayer.data.preferences.ListenHistoryPreferences
import io.github.raghavsatyadev.openplayer.di.BackupGson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import co.touchlab.kermit.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Counted listen history, as an array of
 * `{"songId": "12", "song": {title, artist, album, duration} or null, "listens": [[startedAt, endedAt, listenedMs, trackDurationMs, source], ...]}`
 * with one object per song to keep entry count and size low.
 *
 * On restore, songs are matched against the local library using [songResolver]. A song that cannot
 * be matched is dropped along with its listens.
 */
@Singleton
class ListenHistoryModuleHandler @Inject constructor(
    private val dao: ListenHistoryDao,
    private val songResolver: BackupSongResolver,
    private val preferences: ListenHistoryPreferences,
    @BackupGson private val gson: Gson
) : BackupModuleHandler {

    override val section = BackupSection.LISTEN_HISTORY

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val rows = dao.getAllOnce()
        val grouped = rows.groupBy { it.songId }
        val metadata = songResolver.metadataFor(grouped.keys)
        val array = JsonArray()
        grouped.forEach { (songId, listens) ->
            array.add(toJson(songId, listens, metadata[songId], includeSongField = true))
        }
        gson.toJson(array)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) { dao.count() }

    // Rollback restores this device's own rows, so their ids need no matching.
    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val rows = dao.getAllOnce()
        val array = JsonArray()
        rows.groupBy { it.songId }.forEach { (songId, listens) ->
            array.add(toJson(songId, listens, meta = null, includeSongField = false))
        }
        gson.toJson(array)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val json = JsonParser.parseString(payload)
        if (!json.isJsonArray) return@withContext
        val library = songResolver.library()
        var dropped = 0
        val listens = json.asJsonArray
            .filter { it.isJsonObject }
            .flatMap { parseSong(it.asJsonObject, library) { count -> dropped += count } }
        if (dropped > 0) Logger.withTag(TAG).i { "Listen history restore: $dropped listens dropped, not found" }
        dao.replaceAll(listens)
        preferences.markImported()
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val json = JsonParser.parseString(snapshot)
        if (!json.isJsonArray) return@withContext
        val listens = json.asJsonArray
            .filter { it.isJsonObject }
            .flatMap { parseSong(it.asJsonObject, library = null) {} }
        dao.replaceAll(listens)
        preferences.markImported()
    }

    private fun toJson(
        songId: String,
        listens: List<ListenHistoryEntity>,
        meta: BackupSongMetadata? = null,
        includeSongField: Boolean = false
    ): JsonObject = JsonObject().apply {
        addProperty("songId", songId)
        if (includeSongField) {
            add("song", meta?.let { gson.toJsonTree(it) } ?: JsonNull.INSTANCE)
        }
        val listensArray = JsonArray()
        listens.forEach { listen ->
            val listenArray = JsonArray().apply {
                add(listen.startedAt)
                add(listen.endedAt)
                add(listen.listenedMs)
                add(listen.trackDurationMs)
                add(listen.source)
            }
            listensArray.add(listenArray)
        }
        add("listens", listensArray)
    }

    @Suppress("SENSELESS_COMPARISON", "UNNECESSARY_SAFE_CALL")
    private fun parseSong(
        obj: JsonObject,
        library: BackupSongResolver.Library?,
        onDropped: (Int) -> Unit
    ): List<ListenHistoryEntity> {
        val songId = obj.get("songId")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
        if (songId.isEmpty()) return emptyList()

        val meta = obj.get("song")?.takeIf { it.isJsonObject }
            ?.let { runCatching { gson.fromJson(it, BackupSongMetadata::class.java) }.getOrNull() }
            ?.takeIf { it.title != null && it.artist != null }
            ?.let { it.copy(album = it.album ?: "") }

        val resolved = when {
            library == null -> songId
            meta != null -> library.resolve(songId, meta)
            (songId.toLongOrNull() ?: 0L) > 0L -> library.resolve(songId, null)
            else -> songId
        }

        val listensArray = obj.get("listens")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        val parsedListens = mutableListOf<ParsedListen>()
        for (element in listensArray) {
            if (!element.isJsonArray) continue
            val arr = element.asJsonArray
            if (arr.size() < 5) continue
            val startedAt = runCatching { arr[0].asLong }.getOrNull() ?: continue
            val endedAt = runCatching { arr[1].asLong }.getOrNull() ?: continue
            val listenedMs = runCatching { arr[2].asLong }.getOrNull() ?: continue
            val trackDurationMs = runCatching { arr[3].asLong }.getOrNull() ?: continue
            val source = runCatching { arr[4].asInt }.getOrNull() ?: continue
            parsedListens.add(ParsedListen(startedAt, endedAt, listenedMs, trackDurationMs, source))
        }

        if (resolved == null) {
            onDropped(parsedListens.size)
            return emptyList()
        }

        return parsedListens.map {
            ListenHistoryEntity(
                id = 0L,
                songId = resolved,
                startedAt = it.startedAt,
                endedAt = it.endedAt,
                listenedMs = it.listenedMs,
                trackDurationMs = it.trackDurationMs,
                source = it.source
            )
        }
    }

    private data class ParsedListen(
        val startedAt: Long,
        val endedAt: Long,
        val listenedMs: Long,
        val trackDurationMs: Long,
        val source: Int
    )

    private companion object {
        const val TAG = "ListenHistoryBackup"
    }
}
