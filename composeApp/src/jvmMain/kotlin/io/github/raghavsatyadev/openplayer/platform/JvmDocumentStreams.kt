package io.github.raghavsatyadev.openplayer.platform

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

class JvmDocumentStreams : DocumentStreams {
    private fun file(uri: String): File = if (uri.startsWith("file:")) File(URI(uri)) else File(uri)

    override fun openInput(uri: String): InputStream? = file(uri).takeIf { it.isFile }?.inputStream()

    override fun openOutput(uri: String, mode: String): OutputStream? =
        FileOutputStream(file(uri).apply { parentFile?.mkdirs() }, mode.contains('a'))

    override fun displayName(uri: String): String? = file(uri).name

    override fun delete(uri: String): Int = if (file(uri).delete()) 1 else 0
}
