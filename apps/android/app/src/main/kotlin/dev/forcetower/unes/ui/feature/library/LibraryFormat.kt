package dev.forcetower.unes.ui.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import dev.forcetower.unes.R
import java.text.NumberFormat
import java.time.Instant as JavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

// Locale-aware formatting for the catalogue screens — the Android analogue of
// iOS `LibraryFormat`. Counts group ("1.234"), dates render short day+month
// ("11 ago"), and freshness stamps climb the same ladder as iOS: "agora
// mesmo" → minutes → hours → "ontem, HH:mm".

@Composable
internal fun formatLibraryCount(value: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    return NumberFormat.getIntegerInstance(locale).format(value.toLong())
}

@Composable
internal fun formatLibraryDate(instant: Instant): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
    return JavaInstant.ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .format(formatter)
        .replace(".", "")
}

@Composable
internal fun formatLibraryYear(instant: Instant): Int =
    JavaInstant.ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .year

@Composable
internal fun formatLibraryAgo(checkedAt: Instant, now: Instant): String {
    val minutes = ((now - checkedAt).inWholeMinutes).coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.library_ago_now)
        minutes < 60 -> stringResource(R.string.library_ago_minutes, minutes)
        minutes < 60 * 20 -> stringResource(R.string.library_ago_hours, minutes / 60)
        else -> {
            val locale = LocalConfiguration.current.locales[0]
            val time = JavaInstant.ofEpochMilli(checkedAt.toEpochMilliseconds())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm", locale))
            stringResource(R.string.library_ago_yesterday, time)
        }
    }
}
