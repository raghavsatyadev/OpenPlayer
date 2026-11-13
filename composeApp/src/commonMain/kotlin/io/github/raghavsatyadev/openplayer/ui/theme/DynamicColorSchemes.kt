package io.github.raghavsatyadev.openplayer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFruitSalad
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant
import io.github.raghavsatyadev.openplayer.data.preferences.AlbumArtPaletteStyle
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ColorSchemePair
import kotlin.math.abs
import kotlin.math.roundToInt

private const val GRAYSCALE_CHROMA_THRESHOLD = 12.0
private const val MAX_GRAYSCALE_CHANNEL_DELTA = 10

fun generateColorSchemeFromSeed(
    seedColor: Color,
    paletteStyle: AlbumArtPaletteStyle = AlbumArtPaletteStyle.default
): ColorSchemePair {
    return runCatching {
        val seedArgb = seedColor.toArgb()
        val sourceHct = Hct.fromInt(seedArgb)
        val shouldForceNeutral = shouldUseNeutralArtworkScheme(seedArgb, sourceHct)

        val lightScheme = createDynamicScheme(
            sourceHct = sourceHct,
            paletteStyle = paletteStyle,
            isDark = false
        ).toComposeColorScheme()
        val darkScheme = createDynamicScheme(
            sourceHct = sourceHct,
            paletteStyle = paletteStyle,
            isDark = true
        ).toComposeColorScheme()
        if (shouldForceNeutral) {
            ColorSchemePair(
                light = lightScheme.toGrayscaleColorScheme(),
                dark = darkScheme.toGrayscaleColorScheme()
            )
        } else {
            ColorSchemePair(lightScheme, darkScheme)
        }
    }.getOrElse {
        ColorSchemePair(LightColorScheme, DarkColorScheme)
    }
}

fun generateMonochromeColorSchemeFromSeed(seedColor: Color): ColorSchemePair {
    return runCatching {
        val sourceHct = Hct.fromInt(seedColor.toArgb())
        ColorSchemePair(
            light = SchemeMonochrome(
                sourceColorHct = sourceHct,
                isDark = false,
                contrastLevel = 0.0,
                specVersion = ColorSpec.SpecVersion.SPEC_2021,
                platform = DynamicScheme.Platform.PHONE
            ).toComposeColorScheme(),
            dark = SchemeMonochrome(
                sourceColorHct = sourceHct,
                isDark = true,
                contrastLevel = 0.0,
                specVersion = ColorSpec.SpecVersion.SPEC_2021,
                platform = DynamicScheme.Platform.PHONE
            ).toComposeColorScheme()
        )
    }.getOrElse {
        ColorSchemePair(LightColorScheme, DarkColorScheme)
    }
}

private fun createDynamicScheme(
    sourceHct: Hct,
    paletteStyle: AlbumArtPaletteStyle,
    isDark: Boolean
): DynamicScheme {
    return when (paletteStyle) {
        AlbumArtPaletteStyle.TONAL_SPOT -> SchemeTonalSpot(
            sourceColorHct = sourceHct,
            isDark = isDark,
            contrastLevel = 0.0,
            specVersion = ColorSpec.SpecVersion.SPEC_2021,
            platform = DynamicScheme.Platform.PHONE
        )
        AlbumArtPaletteStyle.VIBRANT -> SchemeVibrant(
            sourceColorHct = sourceHct,
            isDark = isDark,
            contrastLevel = 0.0,
            specVersion = ColorSpec.SpecVersion.SPEC_2021,
            platform = DynamicScheme.Platform.PHONE
        )
        AlbumArtPaletteStyle.EXPRESSIVE -> SchemeExpressive(
            sourceColorHct = sourceHct,
            isDark = isDark,
            contrastLevel = 0.0,
            specVersion = ColorSpec.SpecVersion.SPEC_2021,
            platform = DynamicScheme.Platform.PHONE
        )
        AlbumArtPaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(
            sourceColorHct = sourceHct,
            isDark = isDark,
            contrastLevel = 0.0,
            specVersion = ColorSpec.SpecVersion.SPEC_2021,
            platform = DynamicScheme.Platform.PHONE
        )
    }
}

private fun shouldUseNeutralArtworkScheme(argb: Int, sourceHct: Hct): Boolean {
    return sourceHct.chroma <= GRAYSCALE_CHROMA_THRESHOLD &&
        isArgbNearGrayscale(argb)
}

internal fun isArgbNearGrayscale(argb: Int): Boolean {
    val red = (argb ushr 16) and 0xFF
    val green = (argb ushr 8) and 0xFF
    val blue = argb and 0xFF
    return maxOf(
        abs(red - green),
        abs(green - blue),
        abs(red - blue)
    ) <= MAX_GRAYSCALE_CHANNEL_DELTA
}

private fun argbToHsl(argb: Int, out: FloatArray) {
    val r = (argb ushr 16) and 0xFF
    val g = (argb ushr 8) and 0xFF
    val b = argb and 0xFF

    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f

    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val deltaMaxMin = max - min

    var h: Float
    val s: Float
    val l = (max + min) / 2f

    if (max == min) {
        h = 0f
        s = 0f
    } else {
        if (max == rf) {
            h = ((gf - bf) / deltaMaxMin) % 6f
        } else if (max == gf) {
            h = ((bf - rf) / deltaMaxMin) + 2f
        } else {
            h = ((rf - gf) / deltaMaxMin) + 4f
        }

        s = deltaMaxMin / (1f - abs(2f * l - 1f))
    }

    h = (h * 60f) % 360f
    if (h < 0f) {
        h += 360f
    }

    out[0] = h.coerceIn(0f, 360f)
    out[1] = s.coerceIn(0f, 1f)
    out[2] = l.coerceIn(0f, 1f)
}

private fun hslToArgb(hsl: FloatArray): Int {
    val h = hsl[0]
    val s = hsl[1]
    val l = hsl[2]

    val c = (1f - abs(2f * l - 1f)) * s
    val m = l - 0.5f * c
    val x = c * (1f - abs((h / 60f % 2f) - 1f))

    val hueSegment = (h / 60f).toInt()

    var r = 0
    var g = 0
    var b = 0

    when (hueSegment) {
        0 -> {
            r = (255f * (c + m)).roundToInt()
            g = (255f * (x + m)).roundToInt()
            b = (255f * m).roundToInt()
        }
        1 -> {
            r = (255f * (x + m)).roundToInt()
            g = (255f * (c + m)).roundToInt()
            b = (255f * m).roundToInt()
        }
        2 -> {
            r = (255f * m).roundToInt()
            g = (255f * (c + m)).roundToInt()
            b = (255f * (x + m)).roundToInt()
        }
        3 -> {
            r = (255f * m).roundToInt()
            g = (255f * (x + m)).roundToInt()
            b = (255f * (c + m)).roundToInt()
        }
        4 -> {
            r = (255f * (x + m)).roundToInt()
            g = (255f * m).roundToInt()
            b = (255f * (c + m)).roundToInt()
        }
        5, 6 -> {
            r = (255f * (c + m)).roundToInt()
            g = (255f * m).roundToInt()
            b = (255f * (x + m)).roundToInt()
        }
    }

    r = r.coerceIn(0, 255)
    g = g.coerceIn(0, 255)
    b = b.coerceIn(0, 255)

    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

private fun ColorScheme.toGrayscaleColorScheme(): ColorScheme {
    fun convert(color: Color): Color {
        val hsl = FloatArray(3)
        argbToHsl(color.toArgb(), hsl)
        hsl[0] = 0f
        hsl[1] = 0f
        return Color(hslToArgb(hsl))
    }

    return copy(
        primary = convert(primary),
        onPrimary = convert(onPrimary),
        primaryContainer = convert(primaryContainer),
        onPrimaryContainer = convert(onPrimaryContainer),
        inversePrimary = convert(inversePrimary),
        secondary = convert(secondary),
        onSecondary = convert(onSecondary),
        secondaryContainer = convert(secondaryContainer),
        onSecondaryContainer = convert(onSecondaryContainer),
        tertiary = convert(tertiary),
        onTertiary = convert(onTertiary),
        tertiaryContainer = convert(tertiaryContainer),
        onTertiaryContainer = convert(onTertiaryContainer),
        background = convert(background),
        onBackground = convert(onBackground),
        surface = convert(surface),
        onSurface = convert(onSurface),
        surfaceVariant = convert(surfaceVariant),
        onSurfaceVariant = convert(onSurfaceVariant),
        surfaceTint = convert(surfaceTint),
        inverseSurface = convert(inverseSurface),
        inverseOnSurface = convert(inverseOnSurface),
        error = convert(error),
        onError = convert(onError),
        errorContainer = convert(errorContainer),
        onErrorContainer = convert(onErrorContainer),
        outline = convert(outline),
        outlineVariant = convert(outlineVariant),
        scrim = convert(scrim),
        surfaceBright = convert(surfaceBright),
        surfaceDim = convert(surfaceDim),
        surfaceContainer = convert(surfaceContainer),
        surfaceContainerHigh = convert(surfaceContainerHigh),
        surfaceContainerHighest = convert(surfaceContainerHighest),
        surfaceContainerLow = convert(surfaceContainerLow),
        surfaceContainerLowest = convert(surfaceContainerLowest),
        primaryFixed = convert(primaryFixed),
        primaryFixedDim = convert(primaryFixedDim),
        onPrimaryFixed = convert(onPrimaryFixed),
        onPrimaryFixedVariant = convert(onPrimaryFixedVariant),
        secondaryFixed = convert(secondaryFixed),
        secondaryFixedDim = convert(secondaryFixedDim),
        onSecondaryFixed = convert(onSecondaryFixed),
        onSecondaryFixedVariant = convert(onSecondaryFixedVariant),
        tertiaryFixed = convert(tertiaryFixed),
        tertiaryFixedDim = convert(tertiaryFixedDim),
        onTertiaryFixed = convert(onTertiaryFixed),
        onTertiaryFixedVariant = convert(onTertiaryFixedVariant)
    )
}

private fun DynamicScheme.toComposeColorScheme(): ColorScheme {
    val c = MaterialDynamicColors()
    return ColorScheme(
        primary = Color(getArgb(c.primary)),
        onPrimary = Color(getArgb(c.onPrimary)),
        primaryContainer = Color(getArgb(c.primaryContainer)),
        onPrimaryContainer = Color(getArgb(c.onPrimaryContainer)),
        inversePrimary = Color(getArgb(c.inversePrimary)),
        secondary = Color(getArgb(c.secondary)),
        onSecondary = Color(getArgb(c.onSecondary)),
        secondaryContainer = Color(getArgb(c.secondaryContainer)),
        onSecondaryContainer = Color(getArgb(c.onSecondaryContainer)),
        tertiary = Color(getArgb(c.tertiary)),
        onTertiary = Color(getArgb(c.onTertiary)),
        tertiaryContainer = Color(getArgb(c.tertiaryContainer)),
        onTertiaryContainer = Color(getArgb(c.onTertiaryContainer)),
        background = Color(getArgb(c.background)),
        onBackground = Color(getArgb(c.onBackground)),
        surface = Color(getArgb(c.surface)),
        onSurface = Color(getArgb(c.onSurface)),
        surfaceVariant = Color(getArgb(c.surfaceVariant)),
        onSurfaceVariant = Color(getArgb(c.onSurfaceVariant)),
        surfaceTint = Color(getArgb(c.surfaceTint)),
        inverseSurface = Color(getArgb(c.inverseSurface)),
        inverseOnSurface = Color(getArgb(c.inverseOnSurface)),
        error = Color(getArgb(c.error)),
        onError = Color(getArgb(c.onError)),
        errorContainer = Color(getArgb(c.errorContainer)),
        onErrorContainer = Color(getArgb(c.onErrorContainer)),
        outline = Color(getArgb(c.outline)),
        outlineVariant = Color(getArgb(c.outlineVariant)),
        scrim = Color(getArgb(c.scrim)),
        surfaceBright = Color(getArgb(c.surfaceBright)),
        surfaceDim = Color(getArgb(c.surfaceDim)),
        surfaceContainer = Color(getArgb(c.surfaceContainer)),
        surfaceContainerHigh = Color(getArgb(c.surfaceContainerHigh)),
        surfaceContainerHighest = Color(getArgb(c.surfaceContainerHighest)),
        surfaceContainerLow = Color(getArgb(c.surfaceContainerLow)),
        surfaceContainerLowest = Color(getArgb(c.surfaceContainerLowest)),
        primaryFixed = Color(getArgb(c.primaryFixed)),
        primaryFixedDim = Color(getArgb(c.primaryFixedDim)),
        onPrimaryFixed = Color(getArgb(c.onPrimaryFixed)),
        onPrimaryFixedVariant = Color(getArgb(c.onPrimaryFixedVariant)),
        secondaryFixed = Color(getArgb(c.secondaryFixed)),
        secondaryFixedDim = Color(getArgb(c.secondaryFixedDim)),
        onSecondaryFixed = Color(getArgb(c.onSecondaryFixed)),
        onSecondaryFixedVariant = Color(getArgb(c.onSecondaryFixedVariant)),
        tertiaryFixed = Color(getArgb(c.tertiaryFixed)),
        tertiaryFixedDim = Color(getArgb(c.tertiaryFixedDim)),
        onTertiaryFixed = Color(getArgb(c.onTertiaryFixed)),
        onTertiaryFixedVariant = Color(getArgb(c.onTertiaryFixedVariant))
    )
}
