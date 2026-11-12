package io.github.raghavsatyadev.openplayer.data.video

/**
 * Picks which videos count, by folder. An empty include list means every folder.
 * A folder covers everything inside it, and an excluded folder wins over an included one.
 */
object VideoFolderFilter {

    fun apply(videos: List<LocalVideo>, included: Set<String>, excluded: Set<String>): List<LocalVideo> {
        if (included.isEmpty() && excluded.isEmpty()) return videos
        val inc = included.map(::asDir)
        val exc = excluded.map(::asDir)
        return videos.filter { video ->
            val path = video.path
            (inc.isEmpty() || inc.any(path::startsWith)) && exc.none(path::startsWith)
        }
    }

    /** Every folder that holds a video, with its parent folders, sorted. */
    fun foldersOf(videos: List<LocalVideo>): List<String> {
        val folders = sortedSetOf<String>()
        for (video in videos) {
            var dir = VideoNameText.directoryOf(video.path)
            while (dir.isNotEmpty() && dir.count { it == '/' } >= MIN_DEPTH && folders.add(dir)) {
                dir = dir.substringBeforeLast('/', "")
            }
        }
        return folders.toList()
    }

    private fun asDir(path: String): String = path.trimEnd('/') + "/"

    // Stops at /storage/emulated/0, so the list never offers "/storage" itself.
    private const val MIN_DEPTH = 4
}
