package dev.forcetower.unes.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme

private val MelonLightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = SurfaceLight,
    primaryContainer = BrandPeach,
    onPrimaryContainer = InkLight,

    secondary = BrandPlum,
    onSecondary = SurfaceLight,
    secondaryContainer = BrandMagenta,
    onSecondaryContainer = SurfaceLight,

    tertiary = BrandMagenta,
    onTertiary = SurfaceLight,
    tertiaryContainer = BrandPeach,
    onTertiaryContainer = InkLight,

    background = PageBgLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = Surface2Light,
    onSurfaceVariant = Ink2Light,

    surfaceContainerLowest = CardLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = Surface2Light,
    surfaceContainerHigh = Surface3Light,
    surfaceContainerHighest = Surface3Light,

    outline = Ink3Light,
    outlineVariant = Ink4Light,
)

private val MelonDarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = SurfaceDark,
    primaryContainer = BrandPlum,
    onPrimaryContainer = InkDark,

    secondary = BrandMagenta,
    onSecondary = InkDark,
    secondaryContainer = BrandPlum,
    onSecondaryContainer = InkDark,

    tertiary = BrandPeach,
    onTertiary = SurfaceDark,
    tertiaryContainer = BrandPlum,
    onTertiaryContainer = InkDark,

    background = PageBgDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = Surface2Dark,
    onSurfaceVariant = Ink2Dark,

    surfaceContainerLowest = PageBgDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = Surface2Dark,
    surfaceContainerHigh = Surface3Dark,
    surfaceContainerHighest = CardDark,

    outline = Ink3Dark,
    outlineVariant = Ink4Dark,
)

// Resolved darkness of the active MelonTheme. The Configurações screen lets
// the user pin Claro/Escuro regardless of the OS setting, so anything that
// needs "is the app dark right now" (system-bar chrome, lock-screen mocks)
// must read this instead of `isSystemInDarkTheme()`.
val LocalMelonDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MelonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) MelonDarkColors else MelonLightColors
    val melonColors = if (darkTheme) melonColorsDark() else melonColorsLight()

    CompositionLocalProvider(
        LocalMelonColors provides melonColors,
        LocalMelonDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MelonTypography,
            content = content,
        )
    }
}
