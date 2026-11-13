package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/** Multiplatform version of androidx.graphics.shapes toPath: rewinds [path] and fills it with this polygon's outline. */
internal fun RoundedPolygon.toComposePath(path: Path = Path()): Path = cubics.fillPath(path)

/** Multiplatform version of androidx.graphics.shapes Morph.toPath: the shape at [progress], written into [path]. */
internal fun Morph.toComposePath(progress: Float, path: Path = Path()): Path = asCubics(progress).fillPath(path)

private fun List<Cubic>.fillPath(path: Path): Path {
    path.rewind()
    if (isEmpty()) return path
    path.moveTo(first().anchor0X, first().anchor0Y)
    for (cubic in this) {
        path.cubicTo(
            cubic.control0X, cubic.control0Y,
            cubic.control1X, cubic.control1Y,
            cubic.anchor1X, cubic.anchor1Y,
        )
    }
    path.close()
    return path
}
