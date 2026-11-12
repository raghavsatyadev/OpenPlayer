package io.github.raghavsatyadev.openplayer.platform

import android.util.AtomicFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

actual class PlatformAtomicFile actual constructor(baseFile: File) {
    private val delegate = AtomicFile(baseFile)

    actual fun openRead(): FileInputStream = delegate.openRead()
    actual fun startWrite(): FileOutputStream = delegate.startWrite()
    actual fun finishWrite(stream: FileOutputStream) = delegate.finishWrite(stream)
    actual fun failWrite(stream: FileOutputStream) = delegate.failWrite(stream)
}
