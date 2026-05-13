package dev.forcetower.unes.widgets.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import dev.forcetower.unes.widgets.NextClassEntry
import dev.forcetower.unes.widgets.NextClassState
import dev.forcetower.unes.widgets.WidgetTheme
import dev.forcetower.unes.widgets.countdownEyebrow

// 158×158dp · Android Small. Padded mesh card with code, short title, and the
// time/room strip pinned to the bottom. Mirrors `IOSSmall` /
// `NextClassSmallView` after the Android-tweaks pass (radius 28, accent
// stripe handled at the surface level).
//
// Top-level Column children are intentionally kept to four: Glance enforces
// a hard 10-child ceiling per container (RemoteViews limitation), so each
// sub-block (title pair, footer pair) is wrapped in its own nested Column
// to leave headroom for the wider layouts that share these primitives.
@Composable
internal fun NextClassSmallContent(
    entry: NextClassEntry,
    theme: WidgetTheme,
) {
    val subjectColor = Color(entry.subjectColorArgb)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(14.dp),
    ) {
        EyebrowRow(
            text = if (entry.state == NextClassState.InClass) "agora" else countdownEyebrow(entry),
            fontSize = 8.5f,
            color = theme.ink3,
        )

        Spacer(modifier = GlanceModifier.fillMaxWidth().defaultWeight())

        Column {
            Text(
                text = entry.code,
                style = WidgetText.mono(9.5f, FontWeight.Medium, subjectColor),
                maxLines = 1,
            )
            Text(
                modifier = GlanceModifier.padding(top = 4.dp),
                text = entry.shortTitle,
                style = WidgetText.serif(24f, theme.ink),
                maxLines = 2,
            )
        }

        Column(modifier = GlanceModifier.padding(top = 10.dp)) {
            HairlineDivider(theme)
            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = entry.startTime,
                    style = WidgetText.mono(10f, FontWeight.Normal, theme.ink3),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = entry.room,
                    style = WidgetText.mono(10f, FontWeight.Normal, theme.ink3),
                    maxLines = 1,
                )
            }
        }
    }
}
