package com.minimate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.minimate.R

/**
 * Mont, the app's typeface, in the three weights available: Light, Regular and Black.
 *
 * Compose resolves a requested weight to the nearest supplied one, so asking for Medium or
 * SemiBold lands on Regular and asking for Bold lands on Black. That is the intended behaviour
 * here rather than a compromise — the family has no middle weights, and the jump from Regular to
 * Black is what gives the interface its contrast.
 */
val Mont = FontFamily(
    Font(R.font.mont_light, FontWeight.Light),
    Font(R.font.mont_regular, FontWeight.Normal),
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
