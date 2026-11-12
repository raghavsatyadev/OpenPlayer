package io.github.raghavsatyadev.openplayer.data.preferences

import io.github.raghavsatyadev.openplayer.resources.Res
import io.github.raghavsatyadev.openplayer.resources.*
import org.jetbrains.compose.resources.StringResource

enum class CollagePattern(
    val storageKey: String,
    val labelRes: StringResource
) {
    COSMIC_SWIRL("cosmic_swirl", Res.string.collage_pattern_cosmic_swirl),
    HONEYCOMB_GROOVE("honeycomb_groove", Res.string.collage_pattern_honeycomb_groove),
    VINYL_STACK("vinyl_stack", Res.string.collage_pattern_vinyl_stack),
    PIXEL_MOSAIC("pixel_mosaic", Res.string.collage_pattern_pixel_mosaic),
    STARDUST_SCATTER("stardust_scatter", Res.string.collage_pattern_stardust_scatter),
    SOFT_ORBIT("soft_orbit", Res.string.collage_pattern_soft_orbit),
    ARROW_FANFARE("arrow_fanfare", Res.string.collage_pattern_arrow_fanfare),
    SUNNY_BLOOM("sunny_bloom", Res.string.collage_pattern_sunny_bloom),
    PIXEL_COOKIE("pixel_cookie", Res.string.collage_pattern_pixel_cookie),
    HEART_GARDEN("heart_garden", Res.string.collage_pattern_heart_garden);

    companion object {
        val default: CollagePattern = COSMIC_SWIRL

        fun fromStorageKey(value: String?): CollagePattern {
            return entries.firstOrNull { it.storageKey == value } ?: default
        }
    }
}
