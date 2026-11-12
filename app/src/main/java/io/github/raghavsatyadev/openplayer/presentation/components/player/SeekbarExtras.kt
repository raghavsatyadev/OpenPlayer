package io.github.raghavsatyadev.openplayer.presentation.components.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.preferences.SeekbarStyle
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.AudioBookmarksViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SeekbarViewModel

/** A bookmark of the current song, shown as a chapter. */
@Immutable
data class SeekbarChapter(val timestampMs: Long, val label: String)

/** What the full player's seekbar draws beyond the wavy track. */
@Immutable
class SeekbarExtras(
    /** Null while loading, on failure, or when the wavy style is selected. */
    val waveform: FloatArray?,
    val chapters: List<SeekbarChapter>,
)

@Composable
fun rememberSeekbarExtras(song: Song): SeekbarExtras {
    val seekbarViewModel: SeekbarViewModel = hiltViewModel()
    val bookmarksViewModel: AudioBookmarksViewModel = hiltViewModel()
    val style by seekbarViewModel.style.collectAsState()
    val allBookmarks by bookmarksViewModel.allBookmarks.collectAsState()
    val wantWaveform = style == SeekbarStyle.WAVEFORM
    val waveform by produceState<FloatArray?>(null, song.id, song.path, song.dateModified, wantWaveform) {
        value = null
        if (wantWaveform) {
            seekbarViewModel.waveformFor(song).collect { value = it }
        }
    }
    val chapters = remember(allBookmarks, song.id) {
        allBookmarks.filter { it.songId == song.id }
            .sortedBy { it.timestampMs }
            .map { SeekbarChapter(it.timestampMs, it.title) }
    }
    return remember(waveform, chapters) { SeekbarExtras(waveform, chapters) }
}

/** The chapter that covers [positionMs]: the last one that starts at or before it. */
fun currentChapterLabel(chapters: List<SeekbarChapter>, positionMs: Long): String? =
    chapters.lastOrNull { it.timestampMs <= positionMs }?.label

/** Label of the current chapter under the seekbar; reserves its height so the layout is steady. */
@Composable
fun ChapterLabel(
    chapters: List<SeekbarChapter>,
    positionState: State<Long>,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    if (chapters.isEmpty()) return
    val label by remember(chapters) {
        derivedStateOf { currentChapterLabel(chapters, positionState.value) }
    }
    AnimatedContent(
        targetState = label,
        transitionSpec = {
            (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { -it / 2 })
        },
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .padding(top = 2.dp),
        label = "ChapterLabel"
    ) { text ->
        Text(
            text = text.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
