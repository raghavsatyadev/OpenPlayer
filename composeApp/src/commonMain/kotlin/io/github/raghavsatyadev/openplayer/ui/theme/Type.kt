package io.github.raghavsatyadev.openplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.raghavsatyadev.openplayer.resources.Res
import io.github.raghavsatyadev.openplayer.resources.gflex_variable
import org.jetbrains.compose.resources.Font

@OptIn(ExperimentalTextApi::class)
val MontserratFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.Black,
            variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Black.weight))
        ),
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.ExtraBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.ExtraBold.weight))
        ),
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Bold.weight))
        ),
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.SemiBold.weight))
        ),
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.Medium,
            variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Medium.weight))
        ),
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Normal.weight))
        ),
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.Light,
            variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Light.weight))
        ),
    )

val ExpTitleTypography: Typography
    @Composable
    get() = Typography(
        displayLarge = TextStyle(
            fontFamily = MontserratFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 60.sp,
            textGeometricTransform = TextGeometricTransform(scaleX = 1.5f),
            letterSpacing = (-0.02).em,
            lineHeight = 0.95.em
        ),
        displayMedium = TextStyle(
            fontFamily = MontserratFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 50.sp,
            letterSpacing = (-0.02).em,
            lineHeight = 0.95.em
        ),
        titleMedium = TextStyle(
            fontFamily = MontserratFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            textGeometricTransform = TextGeometricTransform(scaleX = 1.3f),
            letterSpacing = (-0.02).em,
            lineHeight = 0.95.em
        )
    )

private const val RoundedSansFlexRond = 100f

@OptIn(ExperimentalTextApi::class)
val RoundedSans: FontFamily
    @Composable
    get() = FontFamily(
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.Light,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(FontWeight.Light.weight),
                FontVariation.Setting("ROND", RoundedSansFlexRond)
            )
        ),
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(FontWeight.Normal.weight),
                FontVariation.Setting("ROND", RoundedSansFlexRond)
            )
        ),
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.Medium,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(FontWeight.Medium.weight),
                FontVariation.Setting("ROND", RoundedSansFlexRond)
            )
        ),
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(FontWeight.SemiBold.weight),
                FontVariation.Setting("ROND", RoundedSansFlexRond)
            )
        ),
        Font(
            resource = Res.font.gflex_variable,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(FontWeight.Bold.weight),
                FontVariation.Setting("ROND", RoundedSansFlexRond)
            )
        ),
    )

val Typography: Typography
    @Composable
    get() = Typography(
    displayLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
