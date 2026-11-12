package io.github.raghavsatyadev.openplayer.data.database

import org.json.JSONArray

/** Encodes the song id list of [SavedQueueEntity] as a JSON array (order and repeats kept). */
object SavedQueueCodec {
    fun encode(ids: List<String>): String =
        JSONArray().also { array -> ids.forEach { array.put(it) } }.toString()

    fun decode(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { array.optString(it, "").takeIf { id -> id.isNotEmpty() } }
        }.getOrDefault(emptyList())
    }
}
