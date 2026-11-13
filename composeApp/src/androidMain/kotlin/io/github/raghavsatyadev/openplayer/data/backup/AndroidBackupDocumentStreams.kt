package io.github.raghavsatyadev.openplayer.data.backup

import android.content.Context
import androidx.core.net.toUri
import java.io.InputStream
import java.io.OutputStream

class AndroidBackupDocumentStreams(
    private val context: Context,
) : BackupDocumentStreams {
    override fun openOutput(document: String): OutputStream? =
        context.contentResolver.openOutputStream(document.toUri())

    override fun openInput(document: String): InputStream? =
        context.contentResolver.openInputStream(document.toUri())
}
