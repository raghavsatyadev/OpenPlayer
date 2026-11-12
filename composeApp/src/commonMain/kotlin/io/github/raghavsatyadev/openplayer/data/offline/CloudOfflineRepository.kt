package io.github.raghavsatyadev.openplayer.data.offline

import io.github.raghavsatyadev.openplayer.platform.AppDirectories

import io.github.raghavsatyadev.openplayer.data.database.OfflineTrackDao
import io.github.raghavsatyadev.openplayer.data.database.OfflineTrackEntity
import io.github.raghavsatyadev.openplayer.data.model.Song
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class OfflineDownloadStatus(val storageValue: String) {
    QUEUED("queued"),
    DOWNLOADING("downloading"),
    COMPLETE("complete"),
    FAILED("failed");

    companion object {
        fun fromStorage(value: String): OfflineDownloadStatus =
            entries.firstOrNull { it.storageValue == value } ?: FAILED
    }
}

data class OfflineDownload(
    val downloadId: String,
    val sourceUri: String,
    val status: OfflineDownloadStatus,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val localPath: String?,
    val errorMessage: String?,
    val title: String = "",
    val provider: String = ""
) {
    val progress: Float?
        get() = totalBytes?.takeIf { it > 0L }
            ?.let { (bytesDownloaded.toDouble() / it.toDouble()).coerceIn(0.0, 1.0).toFloat() }
}

class CloudOfflineRepository(
    private val appDirectories: AppDirectories,
    private val dao: OfflineTrackDao,
    private val downloadScheduler: CloudDownloadScheduler
) {
    private val mutationMutex = Mutex()

    fun observe(song: Song): Flow<OfflineDownload?> = observe(song.contentUriString)

    fun observe(sourceUri: String): Flow<OfflineDownload?> =
        dao.observeBySourceUri(sourceUri).map { it?.toModel() }

    fun observeCompleted(): Flow<List<OfflineDownload>> =
        dao.observeCompleted().map { rows -> rows.map(OfflineTrackEntity::toModel) }

    fun observeAll(): Flow<List<OfflineDownload>> =
        dao.observeAll().map { rows -> rows.map(OfflineTrackEntity::toModel) }

    suspend fun enqueue(song: Song) = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
            val provider = providerFor(song.contentUriString) ?: return@withLock
            val downloadId = downloadId(song.contentUriString)
            val existing = dao.getBySourceUri(song.contentUriString)
            val completedFileAvailable = existing?.localPath
                ?.let(::File)
                ?.let { it.isFile && it.length() > 0L }
                ?: false
            if (!shouldStartNewAttempt(existing?.state, completedFileAvailable)) {
                return@withLock
            }
            existing?.let {
                it.localPath?.let(::File)?.delete()
                deleteAttemptFiles(appDirectories, it.downloadId, it.attemptId)
            }

            val attemptId = UUID.randomUUID().toString()

            val now = System.currentTimeMillis()
            dao.upsert(
                OfflineTrackEntity(
                    downloadId = downloadId,
                    attemptId = attemptId,
                    songId = song.id,
                    sourceUri = song.contentUriString,
                    provider = provider,
                    title = song.title,
                    mimeType = song.mimeType,
                    localPath = null,
                    state = OfflineDownloadStatus.QUEUED.storageValue,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )

            downloadScheduler.enqueue(downloadId, attemptId, song.contentUriString)
        }
    }

    suspend fun enqueueAll(songs: Collection<Song>) {
        downloadCandidates(songs).forEach { enqueue(it) }
    }

    suspend fun retry(sourceUri: String) = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
            val existing = dao.getBySourceUri(sourceUri) ?: return@withLock
            if (existing.state != OfflineDownloadStatus.FAILED.storageValue) return@withLock

            existing.localPath?.let(::File)?.delete()
            deleteAttemptFiles(appDirectories, existing.downloadId, existing.attemptId)

            val attemptId = UUID.randomUUID().toString()
            dao.upsert(
                existing.copy(
                    attemptId = attemptId,
                    localPath = null,
                    state = OfflineDownloadStatus.QUEUED.storageValue,
                    bytesDownloaded = 0L,
                    totalBytes = null,
                    updatedAt = System.currentTimeMillis(),
                    errorMessage = null
                )
            )
            downloadScheduler.enqueue(existing.downloadId, attemptId, sourceUri)
        }
    }

    suspend fun remove(song: Song) = remove(song.contentUriString)

    suspend fun remove(sourceUri: String) = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
            val entity = dao.getBySourceUri(sourceUri) ?: return@withLock
            // Remove ownership first. Any in-flight worker update after this point is a no-op.
            if (dao.deleteBySourceUriForAttempt(sourceUri, entity.attemptId) == 0) {
                return@withLock
            }

            try {
                downloadScheduler.cancel(entity.downloadId)
            } finally {
                entity.localPath?.let(::File)?.takeIf { it.exists() }?.delete()
                deleteAttemptFiles(appDirectories, entity.downloadId, entity.attemptId)
            }
        }
    }

    /** Called on ExoPlayer's loading thread; Room I/O is dispatched by the caller. */
    suspend fun resolveLocalFile(sourceUri: String): File? = withContext(Dispatchers.IO) {
        val entity = dao.getBySourceUri(sourceUri) ?: return@withContext null
        if (entity.state != OfflineDownloadStatus.COMPLETE.storageValue) return@withContext null
        val file = entity.localPath?.let(::File)
        if (file?.isFile == true && file.length() > 0L) {
            file
        } else {
            dao.deleteBySourceUri(sourceUri)
            null
        }
    }

    companion object {
        fun isCloudSong(song: Song): Boolean = providerFor(song.contentUriString) != null

        fun downloadCandidates(songs: Iterable<Song>): List<Song> = songs.asSequence()
            .filter { isCloudSong(it) }
            .distinctBy(Song::contentUriString)
            .toList()

        fun providerFor(sourceUri: String): String? = when (
            sourceUri.substringBefore(':', "").lowercase(Locale.ROOT)
        ) {
            "navidrome" -> "navidrome"
            "jellyfin" -> "jellyfin"
            else -> null
        }

        fun downloadId(sourceUri: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(sourceUri.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        fun workName(downloadId: String): String = "cloud_track_download_$downloadId"

        /**
         * Album, playlist, and selection actions can all target the same track. Preserve an
         * in-flight attempt instead of replacing it and discarding already-downloaded bytes.
         */
        internal fun shouldStartNewAttempt(
            existingState: String?,
            completedFileAvailable: Boolean
        ): Boolean = when (existingState) {
            OfflineDownloadStatus.QUEUED.storageValue,
            OfflineDownloadStatus.DOWNLOADING.storageValue -> false
            OfflineDownloadStatus.COMPLETE.storageValue -> !completedFileAvailable
            else -> true
        }

        internal fun attemptFileStem(downloadId: String, attemptId: String): String =
            "$downloadId.$attemptId"

        internal fun deleteAttemptFiles(appDirectories: AppDirectories, downloadId: String, attemptId: String) {
            val prefix = "${attemptFileStem(downloadId, attemptId)}."
            downloadDirectory(appDirectories).listFiles()
                ?.asSequence()
                ?.filter { it.name.startsWith(prefix) }
                ?.forEach(File::delete)
        }

        fun downloadDirectory(appDirectories: AppDirectories): File =
            File(appDirectories.filesDir, "cloud_downloads").apply { mkdirs() }
    }
}

private fun OfflineTrackEntity.toModel() = OfflineDownload(
    downloadId = downloadId,
    sourceUri = sourceUri,
    status = OfflineDownloadStatus.fromStorage(state),
    bytesDownloaded = bytesDownloaded,
    totalBytes = totalBytes,
    localPath = localPath,
    errorMessage = errorMessage,
    title = title,
    provider = provider
)
