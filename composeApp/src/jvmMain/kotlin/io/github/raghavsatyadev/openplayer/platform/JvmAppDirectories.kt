package io.github.raghavsatyadev.openplayer.platform

import java.io.File

class JvmAppDirectories(
    root: File = File(System.getProperty("user.home"), ".openplayer"),
) : AppDirectories {
    override val filesDir: File = File(root, "files").apply { mkdirs() }
    override val cacheDir: File = File(root, "cache").apply { mkdirs() }
}
