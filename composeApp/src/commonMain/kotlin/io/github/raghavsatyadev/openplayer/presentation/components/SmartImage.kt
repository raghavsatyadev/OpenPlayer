package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import io.github.raghavsatyadev.openplayer.resources.Res
import io.github.raghavsatyadev.openplayer.resources.ic_music_placeholder
import io.github.raghavsatyadev.openplayer.utils.LocalArtworkUri
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

val SmartImageCompactListTargetSize = Size(96, 96)
val SmartImageListTargetSize = Size(128, 128)
private val DefaultSmartImageSize = Size(300, 300)

@Composable
fun SmartImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: DrawableResource = Res.drawable.ic_music_placeholder,
    error: DrawableResource = Res.drawable.ic_music_placeholder,
    shape: Shape = RectangleShape,
    contentScale: ContentScale = ContentScale.Crop,
    crossfadeDurationMillis: Int = 300,
    useDiskCache: Boolean = true,
    useMemoryCache: Boolean = true,
    allowHardware: Boolean = false,
    targetSize: Size = DefaultSmartImageSize,
    colorFilter: ColorFilter? = null,
    alpha: Float = 1f,
    placeholderModel: Any? = null,
    placeHolderBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onState: ((AsyncImagePainter.State) -> Unit)? = null
) {
    val context = LocalPlatformContext.current
    val clippedModifier = modifier.clip(shape)
    val requestTargetSize = remember(targetSize) {
        safeAlbumArtTargetSize(targetSize)
    }

    val isDirectBitmap = platformBitmapAsImageBitmap(model) != null
    if (model == null || model is ImageVector || model is Painter || model is ImageBitmap || isDirectBitmap) {
        if (model == null) {
            Placeholder(
                modifier = clippedModifier,
                drawable = placeholder,
                contentDescription = contentDescription,
                containerColor = placeHolderBackgroundColor,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                alpha = alpha
            )
        } else {
            handleDirectModel(
                data = model,
                modifier = clippedModifier,
                contentDescription = contentDescription,
                contentScale = contentScale,
                colorFilter = colorFilter,
                alpha = alpha
            )
        }
        return
    }

    val artworkModel = rememberArtworkModelWithCacheVersion(model)
    val request = remember(
        context,
        artworkModel,
        crossfadeDurationMillis,
        useDiskCache,
        useMemoryCache,
        allowHardware,
        requestTargetSize
    ) {
        if (artworkModel is ImageRequest) {
            artworkModel.newBuilder()
                .size(requestTargetSize)
                .build()
        } else {
            ImageRequest.Builder(context)
                .data(artworkModel)
                .crossfade(crossfadeDurationMillis)
                .diskCachePolicy(
                    if (useDiskCache && !LocalArtworkUri.isLocalArtworkUri(artworkModel as? String)) {
                        CachePolicy.ENABLED
                    } else {
                        CachePolicy.DISABLED
                    }
                )
                .memoryCachePolicy(if (useMemoryCache) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .allowHardwareCompat(allowHardware)
                .size(requestTargetSize)
                .build()
        }
    }

    if (onState != null || placeholderModel != null) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = clippedModifier,
            contentScale = contentScale,
            colorFilter = colorFilter,
            alpha = alpha
        ) {
            val state = painter.state.collectAsState().value
            LaunchedEffect(state) {
                onState?.invoke(state)
            }

            when (state) {
                is AsyncImagePainter.State.Success -> {
                    SubcomposeAsyncImageContent()
                }
                is AsyncImagePainter.State.Loading -> {
                    if (placeholderModel != null) {
                        AsyncImage(
                            model = placeholderModel,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = contentScale,
                            colorFilter = colorFilter,
                            alpha = alpha
                        )
                    } else {
                        Placeholder(
                            modifier = Modifier.fillMaxSize(),
                            drawable = placeholder,
                            contentDescription = contentDescription,
                            containerColor = placeHolderBackgroundColor,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            alpha = alpha
                        )
                    }
                }
                else -> {
                    Placeholder(
                        modifier = Modifier.fillMaxSize(),
                        drawable = if (state is AsyncImagePainter.State.Error) error else placeholder,
                        contentDescription = contentDescription,
                        containerColor = placeHolderBackgroundColor,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        alpha = alpha
                    )
                }
            }
        }
    } else {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = clippedModifier,
            contentScale = contentScale,
            colorFilter = colorFilter,
            alpha = alpha,
            placeholder = rememberTintedPainter(painterResource(placeholder), MaterialTheme.colorScheme.outline),
            error = rememberTintedPainter(painterResource(error), MaterialTheme.colorScheme.outline)
        )
    }
}

@Composable
private fun handleDirectModel(
    data: Any?,
    modifier: Modifier,
    contentDescription: String?,
    contentScale: ContentScale,
    colorFilter: ColorFilter?,
    alpha: Float
): Any? {
    return when (data) {
        is ImageVector -> {
            Image(
                imageVector = data,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                colorFilter = colorFilter,
                alpha = alpha
            )
            data
        }
        is Painter -> {
            Image(
                painter = data,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                colorFilter = colorFilter,
                alpha = alpha
            )
            data
        }
        is ImageBitmap -> {
            Image(
                bitmap = data,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                colorFilter = colorFilter,
                alpha = alpha
            )
            data
        }
        else -> {
            val bitmap = platformBitmapAsImageBitmap(data)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale,
                    colorFilter = colorFilter,
                    alpha = alpha
                )
                data
            } else {
                null
            }
        }
    }
}

@Composable
private fun Placeholder(
    modifier: Modifier,
    drawable: DrawableResource,
    contentDescription: String?,
    containerColor: Color,
    iconColor: Color,
    alpha: Float,
) {
    Box(
        modifier = modifier
            .alpha(alpha)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit
        )
    }
}
