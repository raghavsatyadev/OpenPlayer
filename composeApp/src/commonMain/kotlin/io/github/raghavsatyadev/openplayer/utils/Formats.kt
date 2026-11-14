package io.github.raghavsatyadev.openplayer.utils

import androidx.compose.runtime.Composable
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.resources.*
import org.jetbrains.compose.resources.PluralStringResource
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatDuration(milliseconds: Long): String {
    if (milliseconds <= 0L) return "00:00"

    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

// The helpers below are composables so they read the in-app language from the resource
// environment.

@Composable
fun formatTotalDuration(songs: List<Song>): String {
    val totalMillis = songs.sumOf { it.duration }
    val hours = TimeUnit.MILLISECONDS.toHours(totalMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis) % 60
    return if (hours > 0) {
        stringResource(Res.string.duration_hours_minutes_long, hours, minutes)
    } else {
        stringResource(Res.string.duration_minutes_long, minutes)
    }
}

@Composable
fun formatListeningDurationLong(milliseconds: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(Res.string.duration_hours_minutes_medium, hours, minutes)
        hours > 0 -> stringResource(Res.string.duration_hours_medium, hours)
        minutes > 0 -> stringResource(Res.string.duration_minutes_medium, minutes)
        else -> stringResource(Res.string.duration_seconds_medium, seconds)
    }
}

@Composable
fun formatListeningDurationCompact(milliseconds: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(Res.string.duration_hours_minutes_short, hours, minutes)
        hours > 0 -> stringResource(Res.string.duration_hours_short, hours)
        minutes > 0 -> stringResource(Res.string.duration_minutes_short, minutes)
        else -> stringResource(Res.string.duration_seconds_short, seconds)
    }
}

@Composable
fun formatSongCount(count: Int): String =
    pluralStringResource(Res.plurals.song_count, count, count)

@Composable
fun formatTimeAgo(
    timestamp: Long,
    nowMs: Long = System.currentTimeMillis()
): String {
    if (timestamp <= 0) return stringResource(Res.string.time_ago_never)
    // A timestamp slightly in the future (clock skew) reads as just now, not a negative count.
    val diff = (nowMs - timestamp).coerceAtLeast(0L)
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> countString(Res.plurals.time_ago_days, days)
        hours > 0 -> countString(Res.plurals.time_ago_hours, hours)
        minutes > 0 -> countString(Res.plurals.time_ago_minutes, minutes)
        else -> stringResource(Res.string.time_ago_just_now)
    }
}

@Composable
private fun countString(res: PluralStringResource, count: Long): String {
    val quantity = count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return pluralStringResource(res, quantity, quantity)
}
