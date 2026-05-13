package dev.forcetower.unes.widgets.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import dev.forcetower.unes.widgets.NextClassEntry
import dev.forcetower.unes.widgets.WidgetBrand
import dev.forcetower.unes.widgets.WidgetTheme

// 338×158dp "Dia concluído" — flat surface (no mesh), accent-colored "amanhã,
// hh:mm" callout, dotted count of finished classes. Mirrors `StateDayDone` /
// `DayDoneMediumView`. Both themes use the coral accent for the prefix so the
// highlight reads regardless of background.
//
// Body and footer live in nested Columns to stay under Glance's 10-child
// container ceiling; `TomorrowLine` returns a single Column for the same
// reason — its multi-line accent emits would otherwise count as separate
// children of the parent.
@Composable
internal fun DayDoneMediumContent(
    entry: NextClassEntry,
    theme: WidgetTheme,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "◦ TUDO CERTO POR HOJE",
            style = WidgetText.mono(9.5f, FontWeight.Normal, theme.ink3),
            maxLines = 1,
        )

        Column(modifier = GlanceModifier.padding(top = 14.dp)) {
            TomorrowLine(line = entry.dayDoneLine, theme = theme)
            if (entry.title.isNotBlank()) {
                Text(
                    modifier = GlanceModifier.padding(top = 4.dp),
                    text = entry.title,
                    style = WidgetText.sans(12f, theme.ink3),
                    maxLines = 1,
                )
            }
            if (entry.room.isNotBlank()) {
                Text(
                    modifier = GlanceModifier.padding(top = 2.dp),
                    text = entry.room,
                    style = WidgetText.sans(11f, theme.ink4),
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Column {
            HairlineDivider(theme)
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${entry.completedTodayCount} aulas concluídas",
                    style = WidgetText.sans(11f, theme.ink3),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                CompletedDots(count = entry.completedTodayCount)
            }
        }
    }
}

// "Sem aulas até amanhã, 08:00" — coral italic on the prefix part. Glance
// `Text` doesn't support inline span styling, so the prose stacks as two
// `Text` runs inside a Column. Wrapped in a single Composable so the parent
// only sees one child regardless of the accent split.
@Composable
private fun TomorrowLine(line: String?, theme: WidgetTheme) {
    val head: String? = line?.let {
        val sep = it.indexOf(" · ")
        if (sep < 0) null else it.substring(0, sep)
    }
    Column {
        if (head == null) {
            Text(
                text = "Sem aulas hoje",
                style = WidgetText.serif(26f, theme.ink),
                maxLines = 2,
            )
        } else {
            Text(
                text = "Sem aulas até",
                style = WidgetText.serif(26f, theme.ink),
                maxLines = 1,
            )
            Text(
                modifier = GlanceModifier.padding(top = 2.dp),
                text = head,
                style = WidgetText.serif(26f, WidgetBrand.coral, italic = true),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompletedDots(count: Int) {
    // Capped at 5 because Glance enforces a 10-child cap per container —
    // five dots emit 9 items (dot + spacer × 4 + dot). Five also matches
    // the visual rhythm of the iOS variant for typical day counts.
    val capped = count.coerceIn(0, 5)
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(capped) { idx ->
            if (idx > 0) Spacer(modifier = GlanceModifier.width(4.dp))
            Box(
                modifier = GlanceModifier
                    .size(6.dp)
                    .background(ColorProvider(WidgetBrand.ok))
                    .cornerRadiusCompat(3.dp),
            ) {}
        }
    }
}
