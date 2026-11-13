package io.github.raghavsatyadev.openplayer.data.backup.module

import co.touchlab.kermit.Logger
import com.google.gson.Gson
import io.github.raghavsatyadev.openplayer.data.model.Playlist
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal const val PENDING_RESTORE_FILE_NAME = "pending_playlists_restore.json"

/** The part of a restore kept in [PENDING_RESTORE_FILE_NAME] until its songs resolve. */
internal data class PendingPlaylistRestore(
    /** The backed-up playlists still missing songs, with their backup song ids. */
    val playlists: List<Playlist>? = null,
    val songMetadata: Map<String, BackupSongMetadata>? = null,
    /** Backup song ids [songMetadata] describes that no library song has matched yet. */
    val unresolvedSongIds: List<String>? = null,
)

internal object PlaylistsBackupPendingRestore {
    private const val TAG = "PlaylistsBackupPendingRestore"

    /**
     * Saves [pending] for [resolvePendingPlaylists], or drops the pending restore when it's null.
     * Callers hold [pendingRestoreMutex].
     *
     * The file is written beside the pending one and then moved over it, so a process killed or
     * a disk filling up midway leaves the earlier pending restore whole instead of cut off.
     *
     * @return false when [pending] couldn't be saved; the earlier pending restore, if any, is
     * left as it was.
     */
    fun writePendingRestore(
        pending: PendingPlaylistRestore?,
        filesDir: File,
        pendingFile: File,
        gson: Gson,
    ): Boolean {
        if (pending == null) {
            if (pendingFile.exists()) pendingFile.delete()
            return true
        }
        val tempFile = File(filesDir, "$PENDING_RESTORE_FILE_NAME.tmp")
        return try {
            FileOutputStream(tempFile).use { out ->
                out.write(gson.toJson(pending).toByteArray())
                out.fd.sync()
            }
            Files.move(
                tempFile.toPath(), pendingFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            )
            Logger.withTag(TAG).i { "Saved pending playlist restore for songs not in the library yet" }
            true
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Failed to save pending playlist restore file" }
            tempFile.delete()
            false
        }
    }

    /**
     * What a restore of [playlists] still has to fill in once the library has the
     * [unresolvedSongIds], or null when no playlist is missing a song that can still resolve.
     */
    fun pendingRestoreOf(
        playlists: List<Playlist>,
        songMetadata: Map<String, BackupSongMetadata>,
        unresolvedSongIds: Set<String>,
    ): PendingPlaylistRestore? {
        val resolvable = resolvableSongIds(unresolvedSongIds, songMetadata)
        val waiting = playlists.filter { playlist -> playlist.songIds.any { it in resolvable } }
        if (waiting.isEmpty()) return null
        val waitingSongIds = waiting.flatMapTo(HashSet()) { it.songIds }
        return PendingPlaylistRestore(
            playlists = waiting,
            songMetadata = songMetadata.filterKeys { it in waitingSongIds },
            unresolvedSongIds = resolvable.filter { it in waitingSongIds }
        )
    }

    /**
     * The [songIds] that [songMetadata] describes, in order. The backup only has metadata for
     * songs that were in its library, so the others can only ever match a library song by id,
     * with nothing to check it against. On the device the backup came from, that can be the
     * right song, such as one on an SD card that was out when the backup was made, which keeps
     * its id when the card is scanned again. On any other device it's whatever song is given
     * that id, and new songs keep taking ids for as long as the restore would wait. The two
     * can't be told apart, so these songs aren't waited on: a restore keeps one only when the
     * library already has its id, and loses the SD card's songs when made before they're
     * scanned. A playlist entry for a song deleted before the backup then doesn't keep the
     * pending restore around forever either.
     */
    fun resolvableSongIds(
        songIds: Collection<String>,
        songMetadata: Map<String, BackupSongMetadata>,
    ): Set<String> = songIds.filterTo(LinkedHashSet()) { it in songMetadata }

    /**
     * [current] plus a copy of its song for each [newlyResolved] position of [backupSongIds], so
     * a song the backup repeats is repeated here too. Each goes right after the copy that the
     * nearest earlier backup position put in the playlist, or first when there is none. Copies
     * the user added before the song resolved count as backup copies instead of getting another.
     * An unedited playlist ends up in backup order; songs the user removed, moved or added
     * stay as they are.
     */
    fun insertNewlyResolvedSongs(
        current: List<String>,
        backupSongIds: List<String>,
        newlyResolved: Set<String>,
        resolvedSongIds: Map<String, String?>,
    ): List<String> {
        val resolved = backupSongIds.map { resolvedSongIds[it] }
        // The copies of each song beyond the ones the backup's earlier resolved positions put
        // there were added by the user.
        val userCopies = HashMap<String, Int>()
        current.forEach { userCopies.merge(it, 1, Int::plus) }
        backupSongIds.forEachIndexed { index, backupId ->
            val songId = resolved[index]
            if (songId != null && backupId !in newlyResolved) userCopies.merge(songId, -1, Int::plus)
        }

        val songIds = current.toMutableList()
        // Which copy of its song each backup position stands for: 1 for the first, and so on.
        val copyNumbers = IntArray(backupSongIds.size)
        val copiesSoFar = HashMap<String, Int>()
        backupSongIds.forEachIndexed { index, backupId ->
            val songId = resolved[index] ?: return@forEachIndexed
            copyNumbers[index] = copiesSoFar.merge(songId, 1, Int::plus) ?: 1
            if (backupId !in newlyResolved) return@forEachIndexed
            val spare = userCopies[songId] ?: 0
            if (spare > 0) {
                userCopies[songId] = spare - 1
                return@forEachIndexed
            }
            val anchor = (index - 1 downTo 0).asSequence()
                .mapNotNull { previous ->
                    resolved[previous]?.let { songIds.indexOfCopy(it, copyNumbers[previous]) }
                }
                .firstOrNull { it >= 0 }
            songIds.add(anchor?.plus(1) ?: 0, songId)
        }
        return songIds
    }

    /** The index of copy [copy] of [songId], of its last copy when there are fewer, or -1. */
    private fun List<String>.indexOfCopy(songId: String, copy: Int): Int {
        var lastIndex = -1
        var seen = 0
        forEachIndexed { index, id ->
            if (id != songId) return@forEachIndexed
            lastIndex = index
            if (++seen == copy) return index
        }
        return lastIndex
    }
}
