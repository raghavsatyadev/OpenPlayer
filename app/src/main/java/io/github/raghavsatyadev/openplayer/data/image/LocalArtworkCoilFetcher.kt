package io.github.raghavsatyadev.openplayer.data.image

import android.net.Uri
import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import io.github.raghavsatyadev.openplayer.data.image.online.OnlineCoverRepository
import io.github.raghavsatyadev.openplayer.utils.AlbumArtUtils
import io.github.raghavsatyadev.openplayer.utils.LocalArtworkUri
import okio.Path.Companion.toPath

class LocalArtworkCoilFetcher(
    private val uri: Uri,
    private val options: Options,
    private val onlineCovers: Lazy<OnlineCoverRepository>
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val songId = LocalArtworkUri.parseSongId(uri.toString()) ?: return null
        val cachedFile = AlbumArtUtils.ensureAlbumArtCachedFile(
            appContext = options.context,
            songId = songId
        )
        if (cachedFile != null) {
            return SourceResult(
                source = coil.decode.ImageSource(
                    file = cachedFile.absolutePath.toPath(),
                    fileSystem = okio.FileSystem.SYSTEM
                ),
                mimeType = null,
                dataSource = coil.decode.DataSource.DISK
            )
        }

        val onlineFile = onlineCovers.value.coverFor(songId)
        if (onlineFile != null) {
            return SourceResult(
                source = coil.decode.ImageSource(
                    file = onlineFile.absolutePath.toPath(),
                    fileSystem = okio.FileSystem.SYSTEM
                ),
                mimeType = null,
                dataSource = coil.decode.DataSource.NETWORK
            )
        }

        return null
    }

    class Factory(
        private val onlineCovers: Lazy<OnlineCoverRepository>
    ) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (LocalArtworkUri.isLocalArtworkUri(data)) {
                LocalArtworkCoilFetcher(data, options, onlineCovers)
            } else {
                null
            }
        }
    }
}
