package io.github.raghavsatyadev.openplayer.platform

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Write-to-temp-then-replace file, mirroring `android.util.AtomicFile`. */
expect class PlatformAtomicFile(baseFile: File) {
    fun openRead(): FileInputStream
    fun startWrite(): FileOutputStream
    fun finishWrite(stream: FileOutputStream)
    fun failWrite(stream: FileOutputStream)
}
