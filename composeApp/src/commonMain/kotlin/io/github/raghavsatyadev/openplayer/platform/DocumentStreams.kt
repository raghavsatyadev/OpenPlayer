package io.github.raghavsatyadev.openplayer.platform

import java.io.InputStream
import java.io.OutputStream

/**
 * Reads, writes and deletes user-picked documents by their string address. On Android the address is a
 * content:// or file:// Uri string (ContentResolver); on desktop it is a file path or file: URI.
 */
interface DocumentStreams {
    fun openInput(uri: String): InputStream?
    fun openOutput(uri: String, mode: String = "w"): OutputStream?
    /** Human-readable file name, or null when it can't be resolved. */
    fun displayName(uri: String): String?
    /** Deletes the document; returns the number of rows/files removed (0 when nothing was deleted). */
    fun delete(uri: String): Int
}
