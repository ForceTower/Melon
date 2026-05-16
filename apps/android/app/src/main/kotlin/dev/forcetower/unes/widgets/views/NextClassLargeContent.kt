package dev.forcetower.unes.widgets.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import dev.forcetower.unes.widgets.NextClassEntry
import dev.forcetower.unes.widgets.WidgetTheme
import dev.forcetower.unes.widgets.countdownEyebrow

// 338×354dp · Android Large. Adds the today timeline strip and a longer topic
// line. Mirrors `IOSLarge` / `NextClassLargeView`.
//
// Body and footer are wrapped in nested Columns to keep the parent under
// Glance's 10-child cap. Without this grouping the Large layout emits 13
// top-level children, and Glance hard-truncates anything past the tenth.
@Composable
internal fun NextClassLargeContent(
    entry: NextClassEntry,
    theme: WidgetTheme,
) {
    val subjectColor = Color(entry.subjectColorArgb)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(18.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EyebrowRow(
                text = countdownEyebrow(entry),
                fontSize = 10f,
                color = theme.ink3,
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "${entry.startTime} – ${entry.endTime}",
                style = WidgetText.mono(10f, FontWeight.Normal, theme.ink4),
                maxLines = 1,
            )
        }

        Column(modifier = GlanceModifier.padding(top = 18.dp)) {
            CodePill(code = entry.code, color = subjectColor, size = PillSize.Lg)
            Text(
                modifier = GlanceModifier.padding(top = 10.dp),
                text = entry.title,
                style = WidgetText.serif(34f, theme.ink),
                maxLines = 2,
            )
            if (!entry.topic.isNullOrBlank()) {
                Text(
                    modifier = GlanceModifier.padding(top = 10.dp),
                    text = "◦ ${entry.topic}",
                    style = WidgetText.sans(13f, theme.ink2, italic = true),
                    maxLines = 2,
                )
            }
        }

        Column(modifier = GlanceModifier.padding(top = 18.dp)) {
            TodayStrip(bars = entry.todayBars, theme = theme)
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Column {
            HairlineDivider(theme)
            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 12.dp)) {
                MetaPair(
                    room = entry.room,
                    prof = entry.prof.takeIf { it.isNotBlank() }?.let { "Prof. $it" } ?: "",
                    theme = theme,
                    fontSize = 12f,
                )
            }
        }
    }
}
