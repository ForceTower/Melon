package dev.forcetower.unes.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Single-face scale (Manrope) — hierarchy comes from weight and negative
// optical tracking, mirroring the 2026 redesign spec: display/headline roles
// are bold with tight tracking (-0.02…-0.04em), body stays regular, labels
// are bold micro-eyebrows with wide tracking (0.10…0.14em) meant to be
// rendered uppercase.
val MelonTypography = Typography(
    // Display — stat/countdown numerals ("10 dias", "23").
    displayLarge = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 46.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.84).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1.14).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.96).sp,
    ),
    // Headline — screen greetings and hero titles.
    headlineLarge = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.54).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.44).sp,
    ),
    // Title — card titles and list-row headlines.
    titleLarge = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.44).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Label — buttons and uppercase eyebrows.
    labelLarge = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.68.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = MelonSans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.1.sp,
    ),
)
