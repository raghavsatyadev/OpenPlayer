package io.github.raghavsatyadev.openplayer.platform

import java.io.File

/** App-private storage. On Android [filesDir] is exactly `Context.filesDir`, so existing files keep their paths. */
interface AppDirectories {
    val filesDir: File
}
