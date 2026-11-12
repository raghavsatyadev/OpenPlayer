package io.github.raghavsatyadev.openplayer.data.waveform

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Small binary files, one per song, under [dir]. The key carries the song id, file size and date
 * modified, so an edited file never reads a stale waveform. Total size is capped to [maxBytes] by
 * deleting the least recently used files first (a read counts as a use).
 */
class WaveformCache(
    private val dir: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) {

    fun read(key: String): FloatArray? {
        val file = fileFor(key)
        if (!file.isFile) return null
        return runCatching {
            DataInputStream(file.inputStream().buffered()).use { input ->
                if (input.readInt() != MAGIC) return@use null
                val n = input.readInt()
                if (n <= 0 || n > MAX_BARS) return@use null
                FloatArray(n) { input.readFloat() }
            }
        }.getOrNull().also { result ->
            if (result != null) file.setLastModified(System.currentTimeMillis()) else file.delete()
        }
    }

    fun write(key: String, bars: FloatArray) {
        runCatching {
            dir.mkdirs()
            val tmp = File(dir, "${fileName(key)}.tmp")
            DataOutputStream(tmp.outputStream().buffered()).use { out ->
                out.writeInt(MAGIC)
                out.writeInt(bars.size)
                bars.forEach { out.writeFloat(it) }
            }
            val target = fileFor(key)
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            trim()
        }
    }

    private fun trim() {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(EXT) } ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return
        for (f in files.sortedBy { it.lastModified() }) {
            if (total <= maxBytes) break
            total -= f.length()
            f.delete()
        }
    }

    private fun fileFor(key: String) = File(dir, fileName(key))

    private fun fileName(key: String) = key.replace(Regex("[^A-Za-z0-9_.-]"), "_") + EXT

    companion object {
        const val DEFAULT_MAX_BYTES = 8L * 1024 * 1024
        private const val EXT = ".wf"
        private const val MAGIC = 0x57465631 // "WFV1"
        private const val MAX_BARS = 4096

        fun keyFor(songId: String, fileSize: Long, dateModified: Long, bars: Int) =
            "${songId}_${fileSize}_${dateModified}_$bars"
    }
}
