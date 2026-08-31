package com.minimate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.minimate.R

/**
 * Mont, the app's typeface, across five weights.
 *
 * Compose picks the nearest supplied weight for anything not listed, so Medium resolves to
 * SemiBold and Bold resolves to Black. With SemiBold present the middle of the range no longer
 * collapses onto Regular, which is what made headings and body text read alike.
 *
 * Thin is genuinely thin at small sizes on a cover display — worth reserving for large type and
 * for surfaces where the text is decorative rather than something to be read at a glance.
 */
val Mont = FontFamily(
    Font(R.font.mont_thin, FontWeight.Thin),
    Font(R.font.mont_light, FontWeight.Light),
    Font(R.font.mont_regular, FontWeight.Normal),
    Font(R.font.mont_semibold, FontWeight.SemiBold),
    Font(R.font.mont_black, FontWeight.Black)
)

val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = Mont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = Mont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = Mont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = Mont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = Mont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        color = TextTertiary
    )
)
