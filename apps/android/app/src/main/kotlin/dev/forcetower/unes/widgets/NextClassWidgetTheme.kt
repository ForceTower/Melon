package dev.forcetower.unes.widgets

import androidx.compose.ui.graphics.Color

// Theme palette for the next-class widget. Mirrors the `LIGHT`/`DARK`
// constants in `screens-widgets.jsx` and the `WidgetTheme` struct in
// `apps/ios/UNESWidgets/WidgetTokens.swift`.
//
// Glance theming is awkward (no Material composition locals reach across
// the RemoteViews boundary), so we resolve the palette eagerly inside the
// receiver based on the host process's UI mode and pass it to each layout
// as a value type.
internal data class WidgetTheme(
    val surface: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val ink4: Color,
    val line: Color,
    val divider: Color,
    val cardLine: Color,
    val veilTop: Color,
    val veilBottom: Color,
    val todayCellBackground: Color,
    val progressTrack: Color,
    val meshIntensity: Float,
    val meshKind: MeshKind,
) {
    companion object {
        val light = WidgetTheme(
            surface = Color(0xFFFBF7F2),
            ink = Color(0xFF1A1420),
            ink2 = Color(0xFF3A2F42),
            ink3 = Color(0xFF6B5E70),
            ink4 = Color(0xFF9C8FA0),
            line = Color(0x141A1420),
            divider = Color(0x261A1420),
            cardLine = Color(0x0F1A1420),
            veilTop = Color(0x8CFBF7F2),
            veilBottom = Color(0xC7FBF7F2),
            todayCellBackground = Color(0x0A1A1420),
            progressTrack = Color(0x1A1A1420),
            meshIntensity = 0.35f,
            meshKind = MeshKind.Sun,
        )

        val dark = WidgetTheme(
            surface = Color(0xFF1A0F28),
            ink = Color(0xFFFBF7F2),
            ink2 = Color(0xEAFBF7F2),
            ink3 = Color(0xC7FBF7F2),
            ink4 = Color(0x8CFBF7F2),
            line = Color(0x26FBF7F2),
            divider = Color(0x33FBF7F2),
            cardLine = Color(0x0FFFFFFF),
            veilTop = Color(0x141A0F28),
            veilBottom = Color(0x8C1A0F28),
            todayCellBackground = Color(0x0FFBF7F2),
            progressTrack = Color(0x1FFBF7F2),
            meshIntensity = 1f,
            meshKind = MeshKind.Cool,
        )
    }
}

internal enum class MeshKind { Sun, Cool }

// Always-on accent + brand palette pulled from the design system. Surfaces a
// flat object so `@Composable`-only readers from `MaterialTheme.melon` aren't
// required inside Glance code.
internal object WidgetBrand {
    val amber = Color(0xFFF4A23C)
    val coral = Color(0xFFE85D4E)
    val ok = Color(0xFF4AA679)
    val accentStripe = Color(0xFF3B9EAE)
}
