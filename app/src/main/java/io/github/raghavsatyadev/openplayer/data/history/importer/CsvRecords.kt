package io.github.raghavsatyadev.openplayer.data.history.importer

import java.io.Reader

/**
 * Minimal quote-aware CSV reader: commas inside quotes, doubled quotes as an escaped quote and
 * line breaks inside quoted fields. Blank lines are skipped. Streams, one record at a time.
 */
internal object CsvRecords {

    fun read(reader: Reader, onRecord: (List<String>) -> Unit) {
        val field = StringBuilder()
        val row = ArrayList<String>()
        var inQuotes = false
        var quoteClosed = false // the last char closed a quote; a following quote re-opens it
        var fieldWasQuoted = false
        var skipLf = false
        var first = true

        fun endField() {
            row += if (fieldWasQuoted) field.toString() else field.toString().trim()
            field.setLength(0)
            fieldWasQuoted = false
        }

        fun endRow() {
            endField()
            if (!(row.size == 1 && row[0].isEmpty())) onRecord(ArrayList(row))
            row.clear()
        }

        val buffer = CharArray(8192)
        while (true) {
            val n = reader.read(buffer)
            if (n < 0) break
            for (i in 0 until n) {
                val c = buffer[i]
                if (first) {
                    first = false
                    if (c == '﻿') continue
                }
                if (skipLf) {
                    skipLf = false
                    if (c == '\n') continue
                }
                if (inQuotes) {
                    if (c == '"') {
                        inQuotes = false
                        quoteClosed = true
                    } else {
                        field.append(c)
                    }
                    continue
                }
                if (quoteClosed) {
                    quoteClosed = false
                    if (c == '"') {
                        field.append('"')
                        inQuotes = true
                        continue
                    }
                }
                when (c) {
                    '"' -> if (field.isBlank()) {
                        field.setLength(0)
                        inQuotes = true
                        fieldWasQuoted = true
                    } else {
                        field.append(c)
                    }
                    ',' -> endField()
                    '\r' -> { skipLf = true; endRow() }
                    '\n' -> endRow()
                    else -> field.append(c)
                }
            }
        }
        if (field.isNotEmpty() || row.isNotEmpty() || fieldWasQuoted) endRow()
    }
}
