package dev.forcetower.unes.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.glance.text.FontFamily

// Home-screen widget theme. Glance renders through RemoteViews, so neither
// `MaterialTheme` nor downloadable fonts reach it: the widget resolves `light`
// or `dark` eagerly from the host's UI mode and passes it down as a value, and
// it renders with system font families.
@Immutable
data class MelonWidgetTheme(
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
    val meshKind: MelonWidgetMeshKind,
) {
    companion object {
        val light = MelonWidgetTheme(
            surface = WidgetCream,
            ink = InkLight,
            ink2 = Ink2Light,
            ink3 = Ink3Light,
            ink4 = Ink4Light,
            line = WidgetLineLight,
            divider = WidgetDividerLight,
            cardLine = WidgetCardLineLight,
            veilTop = WidgetVeilTopLight,
            veilBottom = WidgetVeilBottomLight,
            todayCellBackground = WidgetTodayCellLight,
            progressTrack = WidgetProgressTrackLight,
            meshIntensity = 0.35f,
            meshKind = MelonWidgetMeshKind.Sun,
        )

        val dark = MelonWidgetTheme(
            surface = AlwaysDarkBg,
            ink = WidgetCream,
            ink2 = WidgetInk2Dark,
            ink3 = WidgetInk3Dark,
            ink4 = WidgetInk4Dark,
            line = WidgetLineDark,
            divider = WidgetDividerDark,
            cardLine = WidgetCardLineDark,
            veilTop = WidgetVeilTopDark,
            veilBottom = WidgetVeilBottomDark,
            todayCellBackground = WidgetTodayCellDark,
            progressTrack = WidgetProgressTrackDark,
            meshIntensity = 1f,
            meshKind = MelonWidgetMeshKind.Cool,
        )
    }
}

// Mesh painted behind the widget content: warm in light, cool in dark.
enum class MelonWidgetMeshKind { Sun, Cool }

// Accents shared by both widget themes.
object MelonWidgetBrand {
    val amber: Color = BrandAmber
    val coral: Color = BrandCoral
    val ok: Color = OkFixed
    val accentStripe: Color = PaletteTealLight
}

// Widget counterparts of the app's sans and monospaced faces (plus the serif
// display moments), limited to the families RemoteViews can render.
object MelonWidgetFonts {
    val sans: FontFamily = FontFamily.SansSerif
    val serif: FontFamily = FontFamily.Serif
    val mono: FontFamily = FontFamily.Monospace
}
