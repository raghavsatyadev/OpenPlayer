package io.github.raghavsatyadev.openplayer.platform

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import java.io.InputStream
import java.io.OutputStream

class AndroidDocumentStreams(private val context: Context) : DocumentStreams {
    override fun openInput(uri: String): InputStream? = context.contentResolver.openInputStream(uri.toUri())

    override fun openOutput(uri: String, mode: String): OutputStream? =
        context.contentResolver.openOutputStream(uri.toUri(), mode)

    override fun displayName(uri: String): String? =
        context.contentResolver.query(uri.toUri(), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    override fun delete(uri: String): Int = context.contentResolver.delete(uri.toUri(), null, null)
}
