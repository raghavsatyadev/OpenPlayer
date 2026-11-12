package io.github.raghavsatyadev.openplayer.data.database

import org.json.JSONArray

/** Encodes the mood and tag lists of [SongUserMetaEntity] as JSON arrays; empty is "". */
object SongUserMetaCodec {
    fun encode(values: List<String>): String {
        val clean = normalize(values)
        if (clean.isEmpty()) return ""
        return JSONArray().also { array -> clean.forEach { array.put(it) } }.toString()
    }

    fun decode(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            normalize((0 until array.length()).map { array.optString(it, "") })
        }.getOrDefault(emptyList())
    }

    /** Trimmed, non-blank, de-duplicated ignoring case, in the given order. */
    fun normalize(values: List<String>): List<String> =
        values.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
}
