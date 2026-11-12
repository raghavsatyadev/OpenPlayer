package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Dimension
import coil3.size.Size
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource
import io.github.raghavsatyadev.openplayer.utils.LocalArtworkUri
import org.jetbrains.compose.resources.painterResource

internal const val MaxSafeAlbumArtDimensionPx = 2048
internal val SafeOriginalAlbumArtSize = Size(MaxSafeAlbumArtDimensionPx, MaxSafeAlbumArtDimensionPx)

@OptIn(ExperimentalCoilApi::class, ExperimentalComposeUiApi::class)
@Composable
fun OptimizedAlbumArt(
    uri: Any?,
    title: String,
    modifier: Modifier = Modifier,
    targetSize: Size = SafeOriginalAlbumArtSize,
    placeholderModel: Any? = null
) {
    val artworkModel = rememberArtworkModelWithCacheVersion(uri)
    val context = LocalPlatformContext.current
    val requestTargetSize = remember(targetSize) {
        safeAlbumArtTargetSize(targetSize)
    }
    val isStableLocalArtwork = remember(artworkModel) {
        when (artworkModel) {
            is String -> LocalArtworkUri.isLocalArtworkUri(artworkModel)
            is ImageRequest -> {
                val data = artworkModel.data
                (data as? String)?.let(LocalArtworkUri::isLocalArtworkUri) == true ||
                    platformUriString(data)?.let(LocalArtworkUri::isLocalArtworkUri) == true
            }
            else -> platformUriString(artworkModel)?.let(LocalArtworkUri::isLocalArtworkUri) == true
        }
    }

    if (renderDirectAlbumArt(
            model = artworkModel,
            title = title,
            modifier = modifier
        )
    ) {
        return
    }

    val memoryCacheKey = remember(artworkModel, requestTargetSize) {
        albumArtMemoryCacheKey(artworkModel, requestTargetSize)
    }
    val placeholderMemoryCacheKey: String? = remember(memoryCacheKey, artworkModel) {
        when (artworkModel) {
            is ImageRequest -> artworkModel.placeholderMemoryCacheKey?.key
                ?: artworkModel.memoryCacheKey
                ?: memoryCacheKey
            else -> memoryCacheKey
        }
    }
    val requestModel = remember(context, artworkModel, requestTargetSize) {
        when (artworkModel) {
            is ImageRequest -> artworkModel.newBuilder().apply {
                size(requestTargetSize)
                if (artworkModel.memoryCacheKey == null) {
                    memoryCacheKey(memoryCacheKey)
                }
                placeholderMemoryCacheKey(placeholderMemoryCacheKey)
            }.build()
            else -> ImageRequest.Builder(context)
                .data(artworkModel)
                .crossfade(350)
                .size(requestTargetSize)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(if (isStableLocalArtwork) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .apply {
                    if (memoryCacheKey != null) {
                        memoryCacheKey(memoryCacheKey)
                    }
                    if (placeholderMemoryCacheKey != null) {
                        placeholderMemoryCacheKey(placeholderMemoryCacheKey)
                    }
                }
                .build()
        }
    }
    var lastSuccessPainter by remember(requestModel.data) { mutableStateOf<Painter?>(null) }

    SubcomposeAsyncImage(
        model = requestModel,
        contentDescription = stringResource(Res.string.cd_album_art_of, title),
        modifier = modifier,
        contentScale = ContentScale.Crop,
        onSuccess = { state ->
            lastSuccessPainter = state.painter
        },
        loading = { state ->
            val cachedPainter = state.painter ?: lastSuccessPainter
            if (cachedPainter != null) {
                SubcomposeAsyncImageContent(painter = cachedPainter)
            } else if (placeholderModel != null) {
                SubcomposeAsyncImage(
                    model = placeholderModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { PlaceholderContent(title = title) },
                    error = { PlaceholderContent(title = title) }
                )
            } else {
                PlaceholderContent(title = title)
            }
        },
        error = {
            val cachedPainter = lastSuccessPainter
            if (cachedPainter != null) {
                SubcomposeAsyncImageContent(painter = cachedPainter)
            } else {
                PlaceholderContent(title = title)
            }
        },
        success = {
            SubcomposeAsyncImageContent()
        }
    )
}

@Composable
private fun PlaceholderContent(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_music_placeholder),
            contentDescription = stringResource(Res.string.cd_album_art_placeholder, title),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(96.dp),
            colorFilter = ColorFilter.tint(
                MaterialTheme.colorScheme.onSurfaceVariant
            ),
        )
    }
}

@Composable
private fun renderDirectAlbumArt(
    model: Any?,
    title: String,
    modifier: Modifier
): Boolean {
    return when (model) {
        is ImageRequest -> renderDirectAlbumArt(model.data, title, modifier)
        is ImageVector -> {
            Image(
                imageVector = model,
                contentDescription = stringResource(Res.string.cd_album_art_of, title),
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize()
            )
            true
        }
        is Painter -> {
            Image(
                painter = model,
                contentDescription = stringResource(Res.string.cd_album_art_of, title),
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize()
            )
            true
        }
        is ImageBitmap -> {
            Image(
                bitmap = model,
                contentDescription = stringResource(Res.string.cd_album_art_of, title),
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize()
            )
            true
        }
        else -> {
            val bitmap = platformBitmapAsImageBitmap(model)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(Res.string.cd_album_art_of, title),
                    contentScale = ContentScale.Crop,
                    modifier = modifier.fillMaxSize()
                )
                true
            } else {
                false
            }
        }
    }
}

internal fun safeAlbumArtTargetSize(targetSize: Size): Size {
    return if (targetSize == Size.ORIGINAL) {
        SafeOriginalAlbumArtSize
    } else {
        targetSize
    }
}

internal fun albumArtMemoryCacheKey(model: Any?, targetSize: Size): String? {
    val data = when (model) {
        is ImageRequest -> model.data
        else -> model
    } ?: return null

    val baseKey = when (data) {
        is String -> data.takeIf { it.isNotBlank() }
        else -> platformUriString(data)?.takeIf { it.isNotBlank() }
    } ?: return null

    if (targetSize == Size.ORIGINAL) return baseKey

    val width = (targetSize.width as? Dimension.Pixels)?.px
    val height = (targetSize.height as? Dimension.Pixels)?.px
    return if (width != null && height != null) {
        "${baseKey}_${width}x${height}"
    } else {
        "${baseKey}_${targetSize.width}x${targetSize.height}"
    }
}
