package io.github.raghavsatyadev.openplayer.platform

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual class PlatformAtomicFile actual constructor(private val baseFile: File) {
    private val newFile = File(baseFile.path + ".new")

    actual fun openRead(): FileInputStream = FileInputStream(baseFile)

    actual fun startWrite(): FileOutputStream {
        baseFile.parentFile?.mkdirs()
        return FileOutputStream(newFile)
    }

    actual fun finishWrite(stream: FileOutputStream) {
        stream.fd.sync()
        stream.close()
        try {
            Files.move(newFile.toPath(), baseFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(newFile.toPath(), baseFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    actual fun failWrite(stream: FileOutputStream) {
        runCatching { stream.close() }
        newFile.delete()
    }
}
