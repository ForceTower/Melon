package dev.forcetower.unes.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import dev.forcetower.unes.designsystem.R

// Mirrors iOS `UNESFont`: Inter for sans/UI text, Fraunces for "serif moments"
// (display + headline). Fonts are downloaded from Google Fonts via the GMS
// font provider on first request and cached on-device.
private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val InterFont = GoogleFont("Inter")
private val FrauncesFont = GoogleFont("Fraunces")

internal val MelonSans: FontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
)

internal val MelonSerif: FontFamily = FontFamily(
    Font(googleFont = FrauncesFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = FrauncesFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = FrauncesFont, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = FrauncesFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
)

internal val MelonMono: FontFamily = FontFamily.Monospace
