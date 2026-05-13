package dev.forcetower.unes.widgets.views

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
// Glance ships two overloads named `ColorProvider`: a `(Color)` factory in
// `androidx.glance.unit` for fixed colors, and a `(day, night)` factory in
// `androidx.glance.color` for adaptive ones. The unit-package factory is
// what we want everywhere — the widget already resolves the theme eagerly
// based on the host's UI mode, so the day/night routing happens upstream.
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.forcetower.unes.widgets.NextClassEntry
import dev.forcetower.unes.widgets.WidgetBrand
import dev.forcetower.unes.widgets.WidgetTheme

// Shared text styles + small layout primitives reused across all three widget
// sizes. Centralized so a typography tweak in one place propagates and
// Glance's awkward `TextStyle` import set stays contained.

internal object WidgetText {
    fun mono(
        size: Float,
        weight: FontWeight = FontWeight.Normal,
        color: Color,
    ): TextStyle = TextStyle(
        color = ColorProvider(color),
        fontSize = size.sp,
        fontWeight = weight,
        fontFamily = FontFamily.Monospace,
    )

    fun serif(
        size: Float,
        color: Color,
        italic: Boolean = false,
    ): TextStyle = TextStyle(
        color = ColorProvider(color),
        fontSize = size.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Serif,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    )

    fun sans(
        size: Float,
        color: Color,
        weight: FontWeight = FontWeight.Normal,
        italic: Boolean = false,
    ): TextStyle = TextStyle(
        color = ColorProvider(color),
        fontSize = size.sp,
        fontWeight = weight,
        fontFamily = FontFamily.SansSerif,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    )
}

// `cornerRadius` requires API 31+; Glance falls back silently on older
// systems (square corners) — wrap the call so the rest of the code reads
// as a single unconditional rounded modifier.
internal fun GlanceModifier.cornerRadiusCompat(radius: Dp): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        cornerRadius(radius)
    } else this

// Small monospace pill used to badge the subject code (CALC II / ALGI / ...).
// Mirrors `CodePill` in `screens-widgets.jsx` and the iOS `CodePill`.
@Composable
internal fun CodePill(
    code: String,
    color: Color,
    size: PillSize = PillSize.Sm,
) {
    val fs = if (size == PillSize.Lg) 11f else 9.5f
    val hpad = if (size == PillSize.Lg) 9 else 7
    val vpad = if (size == PillSize.Lg) 4 else 3
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(color.copy(alpha = 0.13f)))
            .cornerRadiusCompat(6.dp)
            .padding(horizontal = hpad.dp, vertical = vpad.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = code, style = WidgetText.mono(fs, FontWeight.Medium, color))
    }
}

internal enum class PillSize { Sm, Lg }

// Pulsing accent dot used as the "live" indicator. Glance has no animation
// surface, so it's a static tinted disc — same hue as the iOS LiveDot, no
// halo because layered Boxes with translucent backgrounds add too much
// weight at the small dot scale.
@Composable
internal fun LiveDot(color: Color = WidgetBrand.amber, dotSize: Int = 5) {
    Box(
        modifier = GlanceModifier
            .size(dotSize.dp)
            .background(ColorProvider(color))
            .cornerRadiusCompat((dotSize / 2 + 1).dp),
    ) {}
}

// Hairline divider matching the iOS `Divider().overlay(theme.line)` pattern.
@Composable
internal fun HairlineDivider(theme: WidgetTheme) {
    Spacer(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ColorProvider(theme.line)),
    )
}

// Vertical 1×10 dp divider used between footer chips. Mirrors iOS
// `theme.divider`.
@Composable
internal fun FooterDot(theme: WidgetTheme) {
    Spacer(
        modifier = GlanceModifier
            .width(1.dp)
            .height(10.dp)
            .background(ColorProvider(theme.divider)),
    )
}

// Mesh-tinted card surface. The mesh bitmap fills the rounded box; children
// render on top via the `content` slot. The bitmap is pre-baked by the
// receiver and reused across the same render pass.
//
// `mesh = false` skips the bitmap entirely and falls back to the theme
// surface color — used by the day-done state, which is intentionally flat
// per the iOS handoff (`apps/ios/UNESWidgets/NextClassEntryView.swift`).
//
// Both the outer Box and the Image take an *explicit* `size(...)` from
// `LocalSize.current`. Without that, RemoteViews can lay out the Image
// child at 0dp before the host re-measures, leaving the ImageView showing
// only the bitmap's center pixel — which on a radial-gradient bitmap looks
// like a uniform flat color. `ContentScale.FillBounds` matches the Glance
// sample for full-bleed background images.
@Composable
internal fun WidgetCardSurface(
    background: Bitmap,
    surface: androidx.compose.ui.graphics.Color,
    size: DpSize,
    cornerRadius: Int = 22,
    accentStripe: Boolean = false,
    mesh: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = GlanceModifier
            .size(size.width, size.height)
            .background(ColorProvider(surface))
            .cornerRadiusCompat(cornerRadius.dp),
    ) {
        if (mesh) {
            Image(
                provider = ImageProvider(background),
                contentDescription = null,
                modifier = GlanceModifier.size(size.width, size.height),
                contentScale = ContentScale.FillBounds,
            )
        }

        if (accentStripe) {
            // 3dp brand-color stripe pinned to the left edge — long-standing
            // Android widget cue. Same effect as iOS `accentStripe`.
            Row(modifier = GlanceModifier.size(size.width, size.height)) {
                Spacer(
                    modifier = GlanceModifier
                        .width(3.dp)
                        .height(size.height)
                        .background(ColorProvider(WidgetBrand.accentStripe)),
                )
            }
        }

        content()
    }
}

// Eyebrow row used at the top of every size: live dot + uppercase mono
// countdown label. Centralized so the spacing/typography decisions stay in
// one place. The label color is parameterized so the in-class layout can
// flip the eyebrow to amber.
@Composable
internal fun EyebrowRow(
    text: String,
    fontSize: Float,
    color: Color,
    dotColor: Color = WidgetBrand.amber,
    weight: FontWeight = FontWeight.Normal,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LiveDot(color = dotColor, dotSize = (fontSize / 2f).toInt().coerceAtLeast(4))
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = text.uppercase(),
            style = WidgetText.mono(fontSize, weight, color),
            maxLines = 1,
        )
    }
}

// Footer chip pair (room · prof) used on Medium and Large. Mirrors the iOS
// `MetaItem` row — flat text with leading dot prefixes since Glance has no
// system icon font equivalent.
@Composable
internal fun MetaPair(
    room: String,
    prof: String,
    theme: WidgetTheme,
    fontSize: Float,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (room.isNotBlank()) {
            Text(
                text = "◦ Sala $room",
                style = WidgetText.sans(fontSize, theme.ink2),
                maxLines = 1,
            )
        }
        if (room.isNotBlank() && prof.isNotBlank()) {
            Spacer(modifier = GlanceModifier.width(10.dp))
            FooterDot(theme)
            Spacer(modifier = GlanceModifier.width(10.dp))
        }
        if (prof.isNotBlank()) {
            Text(
                text = "◦ $prof",
                style = WidgetText.sans(fontSize, theme.ink2),
                maxLines = 1,
            )
        }
    }
}

// "Seu dia" timeline strip on Large. The iOS layout uses a 2-vs-1 flex
// weighting for the next cell, but Glance only exposes equal-share
// `defaultWeight()` inside Row/Column scopes. We approximate by giving the
// Next cell two consecutive equal-weight slots so it ends up roughly twice
// as wide as the others, matching the iOS effect within Glance's layout
// constraints.
@Composable
internal fun TodayStrip(
    bars: List<NextClassEntry.TodayBar>,
    theme: WidgetTheme,
) {
    if (bars.isEmpty()) return
    // Cap at 5 cells: with N bars the row emits 2N-1 children
    // (cell + spacer + cell + spacer + …), and Glance hard-caps each
    // container at 10 children. 5 visible cells fits the strip rhythm
    // and matches what the iOS Large widget shows in practice.
    val visible = if (bars.size > 5) bars.take(5) else bars
    Column {
        Text(
            text = "◦ SEU DIA",
            style = WidgetText.mono(9f, FontWeight.Normal, theme.ink3),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            visible.forEachIndexed { index, bar ->
                if (index > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                TodayCell(bar = bar, theme = theme)
            }
        }
    }
}

@Composable
private fun TodayCell(
    bar: NextClassEntry.TodayBar,
    theme: WidgetTheme,
) {
    val color = Color(bar.colorArgb)
    val isNext = bar.state == NextClassEntry.TodayBar.State.Next
    val isDone = bar.state == NextClassEntry.TodayBar.State.Done
    val fill = if (isNext) color.copy(alpha = 0.19f) else theme.todayCellBackground
    val width = if (isNext) 84.dp else 60.dp
    Box(
        modifier = GlanceModifier
            .width(width)
            .background(ColorProvider(fill))
            .cornerRadiusCompat(10.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column {
            Text(
                text = bar.code,
                style = WidgetText.mono(
                    9f,
                    FontWeight.Medium,
                    color.copy(alpha = if (isDone) 0.4f else 1f),
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(3.dp))
            Text(
                text = bar.time,
                style = WidgetText.mono(
                    9.5f,
                    FontWeight.Normal,
                    theme.ink3.copy(alpha = if (isDone) 0.4f else 1f),
                ),
                maxLines = 1,
            )
        }
    }
}
