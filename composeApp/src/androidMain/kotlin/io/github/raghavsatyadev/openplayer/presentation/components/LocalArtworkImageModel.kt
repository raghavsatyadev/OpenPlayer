package io.github.raghavsatyadev.openplayer.presentation.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import io.github.raghavsatyadev.openplayer.utils.AlbumArtUtils
import io.github.raghavsatyadev.openplayer.utils.LocalArtworkUri

/** Observe only rare local-art invalidations, never playback state or position updates. */
@Composable
internal fun rememberArtworkModelWithCacheVersion(model: Any?): Any? {
    val isLocalArtwork = remember(model) { localArtworkModelUri(model) != null }
    if (!isLocalArtwork) return model

    val version by AlbumArtUtils.artworkCacheVersion.collectAsStateWithLifecycle()
    return remember(model, version) { withLocalArtworkCacheVersion(model, version) }
}

internal fun withLocalArtworkCacheVersion(model: Any?, version: Long): Any? {
    val uri = localArtworkModelUri(model) ?: return model
    val separator = if ('?' in uri) '&' else '?'
    val versionedUri = "$uri${separator}artwork_version=$version"
    if (model !is ImageRequest) return versionedUri

    return model.newBuilder()
        .data(versionedUri)
        .memoryCacheKey(model.memoryCacheKey?.let { key ->
            "${key}_artwork_version_$version"
        })
        // A caller's old placeholder key can otherwise resurrect the invalidated image.
        .placeholderMemoryCacheKey(null as String?)
        .diskCachePolicy(CachePolicy.DISABLED)
        .build()
}

private fun localArtworkModelUri(model: Any?): String? {
    val data = if (model is ImageRequest) model.data else model
    val uri = when (data) {
        is String -> data
        is Uri -> data.toString()
        else -> return null
    }
    return uri.takeIf(LocalArtworkUri::isLocalArtworkUri)
}
