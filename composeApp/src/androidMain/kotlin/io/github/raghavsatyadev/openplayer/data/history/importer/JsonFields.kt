package io.github.raghavsatyadev.openplayer.data.history.importer

import android.util.JsonReader
import android.util.JsonToken

/** Small helpers over [JsonReader] that never throw on null or unexpected value types. */
internal object JsonFields {

    /** The next value as text; null for JSON null; numbers keep their text form. Other types are skipped. */
    fun nextText(reader: JsonReader): String? = when (reader.peek()) {
        JsonToken.STRING, JsonToken.NUMBER -> reader.nextString()
        JsonToken.BOOLEAN -> reader.nextBoolean().toString()
        else -> {
            reader.skipValue()
            null
        }
    }

    /** The next value as a number (numbers or numeric strings); null otherwise. */
    fun nextDouble(reader: JsonReader): Double? = when (reader.peek()) {
        JsonToken.NUMBER -> reader.nextDouble()
        JsonToken.STRING -> reader.nextString().trim().toDoubleOrNull()
        else -> {
            reader.skipValue()
            null
        }
    }

    /**
     * Reads every element of the array at the reader's position, calling [onElement] with the
     * reader positioned at each (it must consume the value).
     */
    inline fun forEachInArray(reader: JsonReader, onElement: (JsonReader) -> Unit) {
        reader.beginArray()
        while (reader.hasNext()) onElement(reader)
        reader.endArray()
    }

    inline fun forEachField(reader: JsonReader, onField: (name: String) -> Unit) {
        reader.beginObject()
        while (reader.hasNext()) onField(reader.nextName())
        reader.endObject()
    }
}
