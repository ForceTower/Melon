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

// 338×158dp · Android Medium (upcoming state). Code pill + full title + topic +
// room/prof footer. Mirrors `IOSMedium` / `NextClassMediumView`.
//
// Body and footer are wrapped in their own nested Columns so the parent
// Column never exceeds Glance's 10-child RemoteViews cap. Vertical spacing
// is expressed via `padding(top = ...)` instead of standalone `Spacer`s
// for the same reason.
@Composable
internal fun NextClassMediumContent(
    entry: NextClassEntry,
    theme: WidgetTheme,
) {
    val subjectColor = Color(entry.subjectColorArgb)
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
                color = theme.ink3,
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
            if (!entry.topic.isNullOrBlank()) {
                Text(
                    modifier = GlanceModifier.padding(top = 4.dp),
                    text = "“${entry.topic}”",
                    style = WidgetText.sans(11f, theme.ink3, italic = true),
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Column {
            HairlineDivider(theme)
            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 10.dp)) {
                MetaPair(
                    room = entry.room,
                    prof = entry.prof,
                    theme = theme,
                    fontSize = 11f,
                )
            }
        }
    }
}
