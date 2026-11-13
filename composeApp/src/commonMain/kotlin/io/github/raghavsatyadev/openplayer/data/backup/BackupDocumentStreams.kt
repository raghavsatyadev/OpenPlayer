package io.github.raghavsatyadev.openplayer.data.backup

import java.io.InputStream
import java.io.OutputStream

interface BackupDocumentStreams {
    fun openOutput(document: String): OutputStream?
    fun openInput(document: String): InputStream?
}
