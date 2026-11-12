package io.github.raghavsatyadev.openplayer.data.image

import android.net.Uri
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import io.github.raghavsatyadev.openplayer.data.image.online.OnlineCoverRepository
import io.github.raghavsatyadev.openplayer.utils.AlbumArtUtils
import io.github.raghavsatyadev.openplayer.utils.LocalArtworkUri
import dagger.Lazy
import okio.Path.Companion.toPath
import javax.inject.Inject

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
            return SourceFetchResult(
                source = ImageSource(
                    file = cachedFile.absolutePath.toPath(),
                    fileSystem = okio.FileSystem.SYSTEM
                ),
                mimeType = null,
                dataSource = DataSource.DISK
            )
        }

        val onlineFile = onlineCovers.get().coverFor(songId)
        if (onlineFile != null) {
            return SourceFetchResult(
                source = ImageSource(
                    file = onlineFile.absolutePath.toPath(),
                    fileSystem = okio.FileSystem.SYSTEM
                ),
                mimeType = null,
                dataSource = DataSource.NETWORK
            )
        }

        return null
    }

    class Factory @Inject constructor(
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
