package dev.forcetower.unes.widgets

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import dev.forcetower.unes.MainActivity
import dev.forcetower.unes.widgets.views.DayDoneMediumContent
import dev.forcetower.unes.widgets.views.InClassMediumContent
import dev.forcetower.unes.widgets.views.NextClassLargeContent
import dev.forcetower.unes.widgets.views.NextClassMediumContent
import dev.forcetower.unes.widgets.views.NextClassSmallContent
import dev.forcetower.unes.widgets.views.WidgetCardSurface
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// "Próxima aula" — one Glance widget exposed in three home-screen sizes that
// the user can resize between. Mirrors `NextClassWidget` on iOS (Small /
// Medium / Large families served from the same provider; the entry view
// branches internally on size and on the upcoming/inClass/dayDone state).
//
// The widget reads `WidgetSnapshot` (written by `WidgetSnapshotPublisher` on
// the host process) and recomputes the time-derived state at every render,
// so a stale snapshot still produces a correct frame — it just sees the
// same allocations positioned around a newer clock. The receiver schedules
// per-minute self-updates via `AlarmManager` while the widget is bound, so
// the countdown / progress bar tick without needing a fresh snapshot push.
internal class NextClassWidget : GlanceAppWidget() {

    // `Exact` recomposes against the real cell size each time the user
    // resizes, instead of snapping to a fixed `Responsive` breakpoint set.
    // Several launchers (Pixel Launcher in particular) report sizes that
    // don't land cleanly on registered breakpoints, which leaves the widget
    // stuck on a single layout regardless of how the user drags the resize
    // handles. Exact mode trades off pre-cached RemoteViews for honest
    // per-size rendering — exactly what the iOS WidgetFamily switch does.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            NextClassWidgetContent()
        }
    }
}

@Composable
private fun NextClassWidgetContent() {
    val context = LocalContext.current
    val size = LocalSize.current

    // Resolve the current entry once per composition. We re-read the snapshot
    // and re-derive state on every Glance render pass — both are cheap (the
    // snapshot is a small JSON file in app filesDir, the derivation is pure)
    // and avoiding caching keeps the widget honest about clock drift between
    // pushes from the host.
    val entry = remember(size) { resolveEntry(context) }
    val theme = remember { resolveTheme(context) }
    val background = remember(theme) { MeshBackgroundBitmap.render(theme) }

    // Day-done is intentionally rendered flat (no mesh) per the iOS handoff
    // — the calmer surface signals "no plans" without the warmth of the
    // upcoming/inClass mesh. Every other state gets the mesh wash.
    val medium = isMediumSize(size)
    val showMesh = !(medium && entry.state == NextClassState.DayDone)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            // Whole-card tap target — opens the host app's MainActivity.
            // Uses the reified `actionStartActivity<T>()` overload because
            // Glance 1.1.x doesn't expose an Intent-accepting variant.
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        WidgetCardSurface(
            background = background,
            surface = theme.surface,
            size = size,
            cornerRadius = 28,
            accentStripe = true,
            mesh = showMesh,
        ) {
            // Branch on size: the three breakpoints map to the three layouts
            // in `screens-widgets.jsx`. Medium has three sub-layouts driven
            // by `entry.state` so the in-class progress bar and dayDone copy
            // can replace the upcoming hero on the same surface.
            when {
                isLargeSize(size) -> NextClassLargeContent(entry, theme)
                medium -> when (entry.state) {
                    NextClassState.Upcoming -> NextClassMediumContent(entry, theme)
                    NextClassState.InClass -> InClassMediumContent(entry, theme)
                    NextClassState.DayDone -> DayDoneMediumContent(entry, theme)
                }
                else -> NextClassSmallContent(entry, theme)
            }
        }
    }

    // The receiver also schedules self-updates via AlarmManager so the
    // countdown ticks even when no fresh snapshot landed. The LaunchedEffect
    // is a no-op for that scheduling (Glance reschedules automatically on
    // every update) — kept here to make explicit that the composable is a
    // single-shot render, not a long-running observer.
    LaunchedEffect(Unit) {}
}

// Width/height thresholds for switching layouts. Picked at the *midpoints*
// between the iOS canonical sizes (small 158, medium 338, large 338×354)
// so a cell-grid bind that lands a few dp shy of the iOS spec still routes
// to the intended layout. Anything wider than ~250dp gets the wider hero;
// anything taller than ~250dp unlocks the today-strip on Large.
private fun isMediumSize(size: DpSize): Boolean =
    size.width >= 250.dp && size.height < 250.dp

private fun isLargeSize(size: DpSize): Boolean =
    size.width >= 250.dp && size.height >= 250.dp

private fun resolveEntry(context: Context): NextClassEntry {
    val snapshot = WidgetSnapshot.load(context) ?: return NextClassEntry.placeholder

    val tz = TimeZone.getDefault()
    val now = Calendar.getInstance(tz)
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val nowDateIso = String.format(
        Locale.US,
        "%04d-%02d-%02d",
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH) + 1,
        now.get(Calendar.DAY_OF_MONTH),
    )
    return snapshot.renderEntry(
        nowDateIso = nowDateIso,
        nowMinutes = nowMinutes,
        weekdayResolver = ::weekdayLabelForIsoDay,
    )
}

// Widgets follow the **system** appearance (not the app's theme override) per
// the iOS handoff — read it from the host context's UI mode rather than
// relying on a Compose theme that doesn't reach across the RemoteViews
// boundary.
private fun resolveTheme(context: Context): WidgetTheme {
    val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return if (mode == Configuration.UI_MODE_NIGHT_YES) WidgetTheme.dark else WidgetTheme.light
}
