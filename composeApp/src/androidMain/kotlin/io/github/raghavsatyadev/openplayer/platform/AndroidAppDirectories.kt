package io.github.raghavsatyadev.openplayer.platform

import android.content.Context
import java.io.File

class AndroidAppDirectories(private val context: Context) : AppDirectories {
    override val filesDir: File get() = context.filesDir
}
