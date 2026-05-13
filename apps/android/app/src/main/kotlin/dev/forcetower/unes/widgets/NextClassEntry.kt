package dev.forcetower.unes.widgets

// What state the widget should render. Drives the eyebrow copy, the live dot,
// and (for `inClass`) the progress bar. Mirrors the iOS `NextClassState` enum
// in `apps/ios/UNESWidgets/NextClassModel.swift`.
internal enum class NextClassState { Upcoming, InClass, DayDone }

// Materialized entry passed to the Glance views. Same field set as the iOS
// `NextClassEntry` so the two stay aligned — derived from `WidgetSnapshot`
// at render time inside `NextClassSnapshotResolver`.
internal data class NextClassEntry(
    val state: NextClassState,
    val code: String,
    val shortCode: String,
    val title: String,
    val shortTitle: String,
    val prof: String,
    val room: String,
    val startsIn: Int,
    val endsIn: Int,
    val totalDurationMin: Int,
    val startTime: String,
    val endTime: String,
    val topic: String?,
    val todayBars: List<TodayBar>,
    val dayDoneLine: String?,
    val completedTodayCount: Int,
    val subjectColorArgb: Int,
) {
    data class TodayBar(
        val code: String,
        val time: String,
        val state: State,
        val colorArgb: Int,
    ) {
        enum class State { Done, Next, Later }
    }

    companion object {
        // Shown in the widget gallery and during initial load before the
        // host app has had a chance to publish a real snapshot. Same fixture
        // as `NextClassEntry.placeholder` on iOS.
        val placeholder = NextClassEntry(
            state = NextClassState.Upcoming,
            code = "CALC II",
            shortCode = "CALC",
            title = "Cálculo Diferencial II",
            shortTitle = "Cálculo II",
            prof = "Adriana Matos",
            room = "MT-14",
            startsIn = 72,
            endsIn = 0,
            totalDurationMin = 100,
            startTime = "10:20",
            endTime = "12:00",
            topic = "Integrais por partes",
            todayBars = listOf(
                TodayBar("ALGI", "08:00", TodayBar.State.Done, 0xFFE85D4E.toInt()),
                TodayBar("CALC", "10:20", TodayBar.State.Next, 0xFF3B9EAE.toInt()),
                TodayBar("LPOO", "14:00", TodayBar.State.Later, 0xFFB23A7A.toInt()),
                TodayBar("FIS2", "16:20", TodayBar.State.Later, 0xFF2D1B4E.toInt()),
            ),
            dayDoneLine = null,
            completedTodayCount = 1,
            subjectColorArgb = 0xFF3B9EAE.toInt(),
        )

        val inClassPlaceholder = placeholder.copy(
            state = NextClassState.InClass,
            startsIn = 0,
            endsIn = 38,
        )

        val dayDonePlaceholder = NextClassEntry(
            state = NextClassState.DayDone,
            code = "ALGI",
            shortCode = "ALGI",
            title = "Algoritmos I",
            shortTitle = "Algoritmos I",
            prof = "",
            room = "Lab LC-03",
            startsIn = 0,
            endsIn = 0,
            totalDurationMin = 0,
            startTime = "08:00",
            endTime = "",
            topic = null,
            todayBars = emptyList(),
            dayDoneLine = "amanhã, 08:00 · Algoritmos I — Lab LC-03",
            completedTodayCount = 4,
            subjectColorArgb = 0xFFE85D4E.toInt(),
        )
    }
}

internal fun formatCountdown(mins: Int): String {
    val h = mins / 60
    val m = mins % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}min"
    }
}

// Top-eyebrow label for the upcoming-style layouts (Small, Medium upcoming,
// Large). Switches copy when a class is in session so the eyebrow doesn't
// read "em 0 min". Mirrors `countdownEyebrow` on iOS.
internal fun countdownEyebrow(entry: NextClassEntry): String = when (entry.state) {
    NextClassState.InClass -> "agora · termina em ${formatCountdown(entry.endsIn)}"
    NextClassState.Upcoming, NextClassState.DayDone -> "em ${formatCountdown(entry.startsIn)}"
}
