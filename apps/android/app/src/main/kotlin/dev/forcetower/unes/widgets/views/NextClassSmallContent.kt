package dev.forcetower.unes.widgets.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
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
// `NextClassSmallView` for the canonical 158² shape, with two Android-only
// tweaks for resized binds:
//   • Title block sits *under* the eyebrow (not floated to just-above the
//     footer like iOS). When the user makes the widget taller, the empty
//     space lands between title and footer instead of between eyebrow and
//     title — which reads as natural "extra breathing room" rather than the
//     content drifting away from the eyebrow.
//   • Title font scales up when the widget grows tall enough that the iOS
//     24sp would feel lost inside a half-empty card, and the topic line is
//     surfaced when there's vertical room for it (iOS Small skips topic
//     because it's locked at 158pt; we have the room when stretched).
//
// Top-level Column children stay under Glance's 10-child cap by grouping
// the title pair and the footer pair into nested Columns.
@Composable
internal fun NextClassSmallContent(
    entry: NextClassEntry,
    theme: WidgetTheme,
) {
    val subjectColor = Color(entry.subjectColorArgb)
    val height = LocalSize.current.height
    val tall = height >= 200.dp
    val veryTall = height >= 280.dp
    val titleSize = when {
        veryTall -> 32f
        tall -> 28f
        else -> 24f
    }

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

        Column(modifier = GlanceModifier.padding(top = if (tall) 16.dp else 12.dp)) {
            Text(
                text = entry.code,
                style = WidgetText.mono(9.5f, FontWeight.Medium, subjectColor),
                maxLines = 1,
            )
            Text(
                modifier = GlanceModifier.padding(top = 4.dp),
                // shortTitle is capped at 14 chars for the iOS canonical
                // 158pt Small. When the user stretches the widget tall, we
                // have the room for the full title — switch to it so the
                // discipline name reads in full instead of "Cálculo Difer…".
                text = if (tall) entry.title else entry.shortTitle,
                style = WidgetText.serif(titleSize, theme.ink),
                maxLines = if (veryTall) 4 else if (tall) 3 else 2,
            )
            // Topic only when there's the vertical room — iOS Small doesn't
            // surface it because WidgetKit pins the family at 158pt; we have
            // the headroom on stretched binds.
            if (tall && !entry.topic.isNullOrBlank()) {
                Text(
                    modifier = GlanceModifier.padding(top = 6.dp),
                    text = "“${entry.topic}”",
                    style = WidgetText.sans(11f, theme.ink3, italic = true),
                    maxLines = 2,
                )
            }
        }

        Spacer(modifier = GlanceModifier.fillMaxWidth().defaultWeight())

        Column {
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
