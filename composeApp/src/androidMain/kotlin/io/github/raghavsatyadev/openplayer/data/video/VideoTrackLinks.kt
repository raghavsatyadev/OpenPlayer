package io.github.raghavsatyadev.openplayer.data.video

import io.github.raghavsatyadev.openplayer.data.model.Song

/**
 * For each video, the track it belongs to: the reverse of [LocalVideoMatcher.bestMatchFor].
 *
 * When several tracks pick the same video, a track whose file name is a sure match for it wins
 * (for example "Song.flac" over a live version tagged with the same title); otherwise the first.
 */
internal fun videoToSongMap(
    matcher: LocalVideoMatcher,
    songs: List<Song>,
    mode: VideoMatchingMode,
    sameFolderOnly: Boolean,
): Map<Long, Song> {
    if (matcher.isEmpty) return emptyMap()
    val result = HashMap<Long, Song>()
    val sure = HashSet<Long>()
    for (song in songs) {
        if (song.path.isBlank()) continue
        val video = matcher.bestMatchFor(song.toMatchInfo(), mode, sameFolderOnly) ?: continue
        if (video.id in sure) continue
        val isSure = LocalVideoMatcher.sureMatches(
            listOf(video.path),
            VideoNameText.filenameWithoutExtension(song.path),
        ).isNotEmpty()
        if (isSure) {
            sure += video.id
            result[video.id] = song
        } else {
            result.putIfAbsent(video.id, song)
        }
    }
    return result
}
