package io.github.raghavsatyadev.openplayer.data.playlist.rules

import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores a [SmartRuleDefinition] as JSON inside a playlist's `source` text, after
 * [SOURCE_PREFIX]. Unknown or missing fields fall back to defaults, so older and newer
 * versions can read each other's playlists.
 */
object SmartRuleCodec {
    /** Starts with `SMART:` so these playlists count as smart ones everywhere. */
    const val SOURCE_PREFIX = "SMART:rules:"

    fun isRuleSource(source: String): Boolean = source.startsWith(SOURCE_PREFIX)

    fun toSource(definition: SmartRuleDefinition): String = SOURCE_PREFIX + encode(definition)

    fun fromSource(source: String): SmartRuleDefinition? {
        if (!isRuleSource(source)) return null
        return decode(source.removePrefix(SOURCE_PREFIX))
    }

    fun encode(definition: SmartRuleDefinition): String {
        val json = JSONObject()
        json.put("match", definition.matchMode.name)
        val array = JSONArray()
        definition.conditions.forEach { c ->
            array.put(
                JSONObject()
                    .put("field", c.field.name)
                    .put("op", c.operator.name)
                    .put("text", c.text)
                    .put("min", c.min)
                    .put("max", c.max)
                    .put("number", c.number)
            )
        }
        json.put("conditions", array)
        definition.sort?.let {
            json.put("sort", JSONObject().put("field", it.field.name).put("desc", it.descending))
        }
        definition.limit?.let { json.put("limit", it) }
        return json.toString()
    }

    fun decode(raw: String): SmartRuleDefinition? = runCatching {
        val json = JSONObject(raw)
        val array = json.optJSONArray("conditions")
        val conditions = (0 until (array?.length() ?: 0)).mapNotNull { index ->
            val item = array?.optJSONObject(index) ?: return@mapNotNull null
            val field = enumOrNull<SmartRuleField>(item.optString("field")) ?: return@mapNotNull null
            val operator = enumOrNull<SmartRuleOperator>(item.optString("op"))
                ?.takeIf { it in field.operators } ?: field.defaultOperator
            SmartCondition(
                id = index.toLong(),
                field = field,
                operator = operator,
                text = item.optString("text", ""),
                min = item.optLong("min", 0L),
                max = item.optLong("max", 0L),
                number = item.optLong("number", 0L),
            )
        }
        val sort = json.optJSONObject("sort")?.let { s ->
            enumOrNull<SmartSortField>(s.optString("field"))?.let { SmartSort(it, s.optBoolean("desc", false)) }
        }
        SmartRuleDefinition(
            matchMode = enumOrNull<SmartMatchMode>(json.optString("match")) ?: SmartMatchMode.ALL,
            conditions = conditions,
            sort = sort,
            limit = if (json.has("limit")) json.optInt("limit").takeIf { it > 0 } else null,
        )
    }.getOrNull()

    private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? =
        enumValues<E>().firstOrNull { it.name == name }
}
