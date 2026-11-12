package io.github.raghavsatyadev.openplayer.data.video

import io.github.raghavsatyadev.openplayer.data.video.VideoSync.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSyncTest {

    @Test
    fun `loops only when video is shorter than 60 percent of the track`() {
        assertTrue(VideoSync.canLoop(videoDurationMs = 10_000, trackDurationMs = 200_000))
        assertFalse(VideoSync.canLoop(videoDurationMs = 150_000, trackDurationMs = 200_000))
        assertFalse(VideoSync.canLoop(videoDurationMs = 0, trackDurationMs = 200_000))
        assertFalse(VideoSync.canLoop(videoDurationMs = 10_000, trackDurationMs = 0))
    }

    @Test
    fun `small drift is left alone`() {
        assertEquals(Decision.InSync, VideoSync.decide(10_000, 10_200, 200_000, loop = false))
    }

    @Test
    fun `large drift seeks the video to the audio`() {
        assertEquals(Decision.SeekTo(10_000), VideoSync.decide(10_000, 11_000, 200_000, loop = false))
        assertEquals(Decision.SeekTo(10_000), VideoSync.decide(10_000, 0, 200_000, loop = false))
    }

    @Test
    fun `looping video targets audio position modulo its length`() {
        assertEquals(Decision.SeekTo(5_000), VideoSync.decide(25_000, 0, 10_000, loop = true))
        assertEquals(Decision.InSync, VideoSync.decide(25_000, 5_100, 10_000, loop = true))
    }

    @Test
    fun `drift across the loop point is measured the short way round`() {
        // Audio at 20.1s -> target 0.1s; video at 9.95s of a 10s clip is only 0.15s away.
        assertEquals(Decision.InSync, VideoSync.decide(20_100, 9_950, 10_000, loop = true))
    }

    @Test
    fun `audio past the end of a non looping video holds the last frame`() {
        assertEquals(Decision.HoldAtEnd, VideoSync.decide(190_000, 179_000, 180_000, loop = false))
        assertNull(VideoSync.targetPositionMs(180_000, 180_000, loop = false))
    }

    @Test
    fun `unknown video length follows the audio`() {
        assertEquals(Decision.SeekTo(42_000), VideoSync.decide(42_000, 0, 0, loop = false))
        assertEquals(0L, VideoSync.targetPositionMs(-50, 1000, loop = false))
    }
}
