package dev.forcetower.unes.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import dev.forcetower.unes.designsystem.R

// The 2026 redesign uses a single face — Manrope — with hierarchy driven by
// weight + optical tracking (no serif display font). Fonts are downloaded from
// Google Fonts via the GMS font provider on first request and cached on-device.
private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val ManropeFont = GoogleFont("Manrope")

internal val MelonSans: FontFamily = FontFamily(
    Font(googleFont = ManropeFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = ManropeFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = ManropeFont, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = ManropeFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
    Font(googleFont = ManropeFont, fontProvider = GoogleFontsProvider, weight = FontWeight.ExtraBold),
    Font(googleFont = ManropeFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
)

internal val MelonMono: FontFamily = FontFamily.Monospace
