package dev.forcetower.unes.widgets.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import dev.forcetower.unes.widgets.NextClassEntry
import dev.forcetower.unes.widgets.WidgetBrand
import dev.forcetower.unes.widgets.WidgetTheme
import dev.forcetower.unes.widgets.countdownEyebrow

// 338×158dp "Aula em andamento" — same hero as Medium but the eyebrow flips
// to a count-down to end and the bottom row is replaced by a static progress
// bar. Mirrors `StateInClass` / `InClassMediumView`. Glance has no animator,
// so the fill is a snapshot of the elapsed fraction at render time; the
// receiver re-renders on `WidgetSnapshot` updates and on the per-minute
// `AlarmManager` tick wired by `NextClassWidgetReceiver`.
//
// Body, progress, and footer rows live inside their own nested Columns so
// the parent stays under Glance's 10-child cap.
@Composable
internal fun InClassMediumContent(
    entry: NextClassEntry,
    theme: WidgetTheme,
) {
    val subjectColor = Color(entry.subjectColorArgb)
    val progress: Float = run {
        val total = entry.totalDurationMin.coerceAtLeast(1)
        val elapsed = (total - entry.endsIn).coerceAtLeast(0)
        (elapsed.toFloat() / total).coerceIn(0f, 1f)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EyebrowRow(
                text = countdownEyebrow(entry),
                fontSize = 9.5f,
                color = WidgetBrand.amber,
                weight = FontWeight.Medium,
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "${entry.startTime} – ${entry.endTime}",
                style = WidgetText.mono(9.5f, FontWeight.Normal, theme.ink4),
                maxLines = 1,
            )
        }

        Column(modifier = GlanceModifier.padding(top = 14.dp)) {
            CodePill(code = entry.code, color = subjectColor)
            Text(
                modifier = GlanceModifier.padding(top = 6.dp),
                text = entry.title,
                style = WidgetText.serif(26f, theme.ink),
                maxLines = 1,
            )
            if (entry.room.isNotBlank()) {
                Text(
                    modifier = GlanceModifier.padding(top = 2.dp),
                    text = entry.room,
                    style = WidgetText.sans(12f, theme.ink3),
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Column {
            ProgressTrack(progress = progress, theme = theme)
            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    text = "${entry.startTime} · iniciada",
                    style = WidgetText.mono(9.5f, FontWeight.Normal, theme.ink3),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${entry.endTime} · final",
                    style = WidgetText.mono(9.5f, FontWeight.Normal, theme.ink3),
                    maxLines = 1,
                )
            }
        }
    }
}

// 5dp progress track + amber fill. Glance has no `Box(width = fraction)` —
// the fill is rendered with a fixed dp width by mapping `progress * track`,
// which we approximate by reading the parent track width as a constant 305dp
// (Medium card minus 16dp horizontal padding = 306dp; rounded down for a
// hairline of safety). Acceptable because the medium widget always renders
// at the same dp width regardless of host density.
@Composable
private fun ProgressTrack(progress: Float, theme: WidgetTheme) {
    val trackDp = 306
    val fillDp = (trackDp * progress).toInt().coerceIn(0, trackDp)
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(5.dp)
            .background(ColorProvider(theme.progressTrack))
            .cornerRadiusCompat(3.dp),
    ) {
        if (fillDp > 0) {
            Spacer(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .width(fillDp.dp)
                    .background(ColorProvider(WidgetBrand.amber))
                    .cornerRadiusCompat(3.dp),
            )
        }
    }
}
