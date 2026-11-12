package io.github.raghavsatyadev.openplayer.data.video

import androidx.compose.runtime.Immutable

/** A video file on the device, from MediaStore. */
@Immutable
data class LocalVideo(
    val id: Long,
    val path: String,
    val contentUri: String,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val mimeType: String? = null,
    /** A "<name>-poster.jpg" image next to the video, as a content URI, when one exists. */
    val posterUri: String? = null,
) {
    /** The shorter side, as in "1080p". 0 when unknown. */
    val resolution: Int get() = if (width > 0 && height > 0) minOf(width, height) else maxOf(width, height)
}

enum class VideoMatchingMode(val storageKey: String) {
    /** Filename, then title + artist (or title + genre). */
    AUTO("auto"),
    FILENAME("filename"),
    TITLE_AND_ARTIST("title_artist");

    companion object {
        fun fromStorageKey(key: String?): VideoMatchingMode = entries.firstOrNull { it.storageKey == key } ?: AUTO
    }
}

/** What a track is matched by. */
data class TrackMatchInfo(
    val path: String,
    val title: String,
    val artist: String?,
    val genre: String?,
)

/**
 * Finds the local videos that belong to a track, by its filename or by its title and artist.
 *
 * The rules follow Namida: every word of the track's text must be in the video's name, and the
 * text's phrases (split at symbols like " - ") must appear as whole runs of words. So
 * "Artist - Title (Official Video) [1080p].mp4" matches title "Title" and artist "Artist", while
 * a video that only shares some of the words does not.
 */
class LocalVideoMatcher(val videos: List<LocalVideo>) {

    private val videosByPath: Map<String, LocalVideo> = videos.associateBy { it.path }
    private val tokenIndex: Map<String, Set<String>>
    private val wordsTextCache = HashMap<String, String>()

    init {
        val index = HashMap<String, MutableSet<String>>()
        for (video in videos) {
            val name = VideoNameText.filenameWithoutExtension(video.path)
            for (token in VideoNameText.tokenizeHaystack(name)) {
                index.getOrPut(token) { LinkedHashSet() }.add(video.path)
            }
        }
        tokenIndex = index
    }

    val isEmpty: Boolean get() = videosByPath.isEmpty()

    /** Videos whose names contain every token of [text]. */
    private fun matchText(text: String): Set<String> {
        var matched: Set<String>? = null
        for (token in VideoNameText.tokenize(text)) {
            val paths = tokenIndex[token] ?: return emptySet()
            matched = matched?.intersect(paths) ?: paths
            if (matched.isEmpty()) return matched
        }
        return matched ?: emptySet()
    }

    private fun wordsTextOf(path: String): String = synchronized(wordsTextCache) {
        wordsTextCache.getOrPut(path) {
            VideoNameText.toWordsText(VideoNameText.filenameWithoutExtension(path))
        }
    }

    /** All videos matching [track], best first. */
    fun matchesFor(
        track: TrackMatchInfo,
        mode: VideoMatchingMode = VideoMatchingMode.AUTO,
        sameFolderOnly: Boolean = false,
    ): List<LocalVideo> {
        if (videosByPath.isEmpty()) return emptyList()
        val matched = LinkedHashSet<String>()

        fun addPhraseMatches(candidates: Set<String>, texts: List<String>) {
            if (candidates.isEmpty()) return
            val query = PhrasesQuery.fromTexts(texts)
            candidates.filterTo(matched) { query.isContainedIn(wordsTextOf(it)) }
        }

        val filename = VideoNameText.filenameWithoutExtension(track.path)

        fun matchFilename() {
            if (filename.isBlank()) return
            addPhraseMatches(matchText(filename), listOf(filename))
        }

        fun matchTitleAndArtist() {
            val title = track.title
            if (title.isBlank()) return
            val titleMatches = matchText(title)
            if (titleMatches.isEmpty()) return
            val artist = track.artist
            if (!artist.isNullOrBlank()) {
                addPhraseMatches(titleMatches.intersect(matchText(artist)), listOf(title, artist))
            }
            // For names like "Nightcore - Title", where the track's first genre is "Nightcore".
            val genre = track.genre
            if (!genre.isNullOrBlank()) {
                addPhraseMatches(titleMatches.intersect(matchText(genre)), listOf(title, genre))
            }
        }

        when (mode) {
            VideoMatchingMode.AUTO -> {
                matchFilename()
                matchTitleAndArtist()
            }
            VideoMatchingMode.FILENAME -> matchFilename()
            VideoMatchingMode.TITLE_AND_ARTIST -> matchTitleAndArtist()
        }

        if (sameFolderOnly) {
            val trackDir = VideoNameText.directoryOf(track.path)
            matched.retainAll { VideoNameText.directoryOf(it) == trackDir }
        }

        val sure = sureMatches(matched, filename)
        return matched.mapNotNull(videosByPath::get).sortedWith(
            compareByDescending<LocalVideo> { if (it.path in sure) 1 else 0 }
                .thenByDescending { it.resolution }
                .thenByDescending { it.sizeBytes }
        )
    }

    /** The best video for [track], or null. */
    fun bestMatchFor(
        track: TrackMatchInfo,
        mode: VideoMatchingMode = VideoMatchingMode.AUTO,
        sameFolderOnly: Boolean = false,
    ): LocalVideo? = matchesFor(track, mode, sameFolderOnly).firstOrNull()

    companion object {
        private val trailingBrackets = Regex("\\s*[(\\[{][^()\\[\\]{}]*[)\\]}]\\s*$")

        /**
         * Videos that surely belong to the track: the video's name is the track's filename, with
         * any number of trailing bracket parts like "(480p)" or "[Official Video]" allowed.
         */
        fun sureMatches(videoPaths: Collection<String>, trackFilenameWithoutExt: String): Set<String> {
            val trackWords = VideoNameText.toWordsText(trackFilenameWithoutExt)
            if (trackWords.isBlank()) return emptySet()
            return videoPaths.filterTo(LinkedHashSet()) { isSureMatch(it, trackWords) }
        }

        private fun isSureMatch(videoPath: String, trackWords: String): Boolean {
            var name = VideoNameText.filenameWithoutExtension(videoPath)
            while (true) {
                if (VideoNameText.toWordsText(name) == trackWords) return true
                val brackets = trailingBrackets.find(name) ?: return false
                name = name.substring(0, brackets.range.first)
            }
        }
    }
}
