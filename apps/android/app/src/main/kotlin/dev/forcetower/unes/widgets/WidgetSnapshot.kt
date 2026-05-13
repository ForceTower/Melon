package dev.forcetower.unes.widgets

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Wire-format snapshot the host process writes to internal storage and the
// widget reads back at render time. Same idea as iOS, except both producer
// and consumer live in the same APK so we share the Kotlin type directly
// instead of mirroring two struct definitions.
//
// Only raw schedule data lives here. Time-derived state (running / upcoming /
// dayDone, countdowns, today bars) is recomputed from `now` at every widget
// update via `renderEntry`, so a stale snapshot still produces a correct
// entry — the widget just sees the same classes positioned around a newer
// clock.
@Serializable
internal data class WidgetSnapshot(
    val generatedAtEpochMs: Long,
    val todayDateIso: String,
    val today: List<Class>,
    val nextDay: NextDay?,
) {
    @Serializable
    data class Class(
        val classId: String,
        val code: String,
        val title: String,
        val prof: String?,
        val room: String?,
        val topic: String?,
        // "HH:mm" — start of the class allocation.
        val startTime: String,
        // "HH:mm" — null when upstream didn't ship one.
        val endTime: String?,
    )

    @Serializable
    data class NextDay(
        // "YYYY-MM-DD".
        val dateIso: String,
        // Distance from today's date in days (1 = tomorrow, 2 = day after, ...).
        val daysAway: Int,
        val first: Class,
    )

    companion object {
        const val FILE_NAME = "next-class-snapshot.json"

        // Format used by both reader and writer; lenient so an extra field
        // shipped by a newer producer doesn't break an older consumer (and
        // vice versa during in-flight upgrades).
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun file(context: Context): File = File(context.filesDir, FILE_NAME)

        fun load(context: Context): WidgetSnapshot? {
            val f = file(context)
            if (!f.exists()) return null
            return runCatching {
                json.decodeFromString(serializer(), f.readText())
            }.getOrNull()
        }

        fun save(context: Context, snapshot: WidgetSnapshot) {
            val f = file(context)
            // Atomic replace: write a sibling tmp file then rename so a
            // half-written JSON never lands on disk for the widget reader to
            // trip over. Same pattern the iOS publisher uses with
            // `Data.write(to:options:.atomic)`.
            val tmp = File(f.parentFile, "${f.name}.tmp")
            tmp.writeText(json.encodeToString(serializer(), snapshot))
            if (!tmp.renameTo(f)) {
                // renameTo can fail across some filesystems — fall back to a
                // copy + delete which is still atomic w.r.t. partial writes
                // because the temp file contains the full payload.
                f.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }
}

// Loads the on-disk snapshot and resolves it against the current wall clock
// into a `NextClassEntry`. Returns null when no snapshot has been written yet
// — caller decides whether to render a placeholder (the Glance widget) or
// fall back to a safe default cadence (the receiver). Shared by the widget
// composable and the receiver's tick scheduler so both branch on the exact
// same materialized entry.
//
// `isDark` flows into `SubjectPalette` so the discipline color baked into
// the entry uses the lifted dark-mode hex when the host renders against a
// dark UI mode. The receiver's tick path is theme-agnostic and can pass
// false (the bar/subject colors aren't read by the alarm scheduler).
internal fun loadCurrentEntry(context: Context, isDark: Boolean = false): NextClassEntry? {
    val snapshot = WidgetSnapshot.load(context) ?: return null
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
        isDark = isDark,
    )
}

// Pure derivation of a `NextClassEntry` from this snapshot at `nowMinutes`
// + `nowDateIso`. Mirrors `WidgetSnapshot.renderEntry` on iOS — same
// running / upcoming / dayDone branch logic, same in-class-handoff threshold,
// same fall-through to `nextDay` so the Small / Large widgets render real
// data even when today is empty.
internal fun WidgetSnapshot.renderEntry(
    nowDateIso: String,
    nowMinutes: Int,
    weekdayResolver: (isoDay: String) -> String? = { null },
    isDark: Boolean = false,
): NextClassEntry {
    val todayIsToday = todayDateIso == nowDateIso
    val currentClasses = if (todayIsToday) today else emptyList()

    val running = currentClasses.firstOrNull { c ->
        val start = parseHhMm(c.startTime) ?: return@firstOrNull false
        val end = parseHhMm(c.endTime) ?: return@firstOrNull false
        nowMinutes in start until end
    }

    val upcoming = currentClasses
        .mapNotNull { c ->
            val start = parseHhMm(c.startTime) ?: return@mapNotNull null
            if (start <= nowMinutes) null else c to start
        }
        .minByOrNull { it.second }
        ?.first

    val completedCount = currentClasses.count { c ->
        val end = parseHhMm(c.endTime) ?: return@count false
        nowMinutes >= end
    }

    val bars = currentClasses.map { c ->
        val start = parseHhMm(c.startTime) ?: 0
        val end = parseHhMm(c.endTime) ?: start
        val state = when {
            nowMinutes >= end -> NextClassEntry.TodayBar.State.Done
            upcoming != null && upcoming.classId == c.classId -> NextClassEntry.TodayBar.State.Next
            running != null && running.classId == c.classId -> NextClassEntry.TodayBar.State.Next
            else -> NextClassEntry.TodayBar.State.Later
        }
        NextClassEntry.TodayBar(
            code = shortCode(c.code),
            time = c.startTime,
            state = state,
            colorArgb = SubjectPalette.argb(c.code, isDark),
        )
    }

    // In the final stretch of a running class we shift attention to the
    // next class so the student gets a heads-up before walking out, but only
    // when there's actually another class to flag. Same 30 min threshold as iOS.
    val inClassSwitchThreshold = 30
    if (running != null) {
        val start = parseHhMm(running.startTime) ?: nowMinutes
        val end = parseHhMm(running.endTime) ?: (nowMinutes + 60)
        val endsIn = (end - nowMinutes).coerceAtLeast(0)
        val handOff = endsIn <= inClassSwitchThreshold && upcoming != null
        if (!handOff) {
            return NextClassEntry(
                state = NextClassState.InClass,
                code = running.code,
                shortCode = shortCode(running.code),
                title = running.title,
                shortTitle = shortTitle(running.title),
                prof = running.prof.orEmpty(),
                room = running.room.orEmpty(),
                startsIn = 0,
                endsIn = endsIn,
                totalDurationMin = (end - start).coerceAtLeast(1),
                startTime = running.startTime,
                endTime = running.endTime.orEmpty(),
                topic = running.topic,
                todayBars = bars,
                dayDoneLine = null,
                completedTodayCount = completedCount,
                subjectColorArgb = SubjectPalette.argb(running.code, isDark),
            )
        }
    }

    if (upcoming != null) {
        val start = parseHhMm(upcoming.startTime) ?: nowMinutes
        val end = parseHhMm(upcoming.endTime) ?: start
        return NextClassEntry(
            state = NextClassState.Upcoming,
            code = upcoming.code,
            shortCode = shortCode(upcoming.code),
            title = upcoming.title,
            shortTitle = shortTitle(upcoming.title),
            prof = upcoming.prof.orEmpty(),
            room = upcoming.room.orEmpty(),
            startsIn = (start - nowMinutes).coerceAtLeast(0),
            endsIn = 0,
            totalDurationMin = (end - start).coerceAtLeast(1),
            startTime = upcoming.startTime,
            endTime = upcoming.endTime.orEmpty(),
            topic = upcoming.topic,
            todayBars = bars,
            dayDoneLine = null,
            completedTodayCount = completedCount,
            subjectColorArgb = SubjectPalette.argb(upcoming.code, isDark),
        )
    }

    // No class left in `currentClasses`. If the snapshot's `nextDay` points
    // at the next populated day, surface it as upcoming so the main subject
    // area on Small / Large renders real data — countdown spans the day
    // boundary. The Medium widget keeps its dedicated dayDone treatment via
    // `dayDoneLine`.
    //
    // Re-derive `daysAway` against render time: `n.daysAway` was computed
    // when the snapshot was written, so it drifts on day rollover before
    // the host writes a fresh snapshot. When the re-derivation lands at
    // 0 — i.e. the next populated day is *today* — emit `Upcoming` instead
    // of `DayDone` so the copy doesn't read "Sem aulas até hoje, 07:30"
    // while a class is genuinely coming up later today.
    val n = nextDay
    if (n != null) {
        val nextStart = parseHhMm(n.first.startTime) ?: 0
        val nextEnd = parseHhMm(n.first.endTime) ?: nextStart
        val daysAway = (daysBetweenIsoDays(nowDateIso, n.dateIso) ?: n.daysAway).coerceAtLeast(0)
        val startsIn = (daysAway * 1440 - nowMinutes + nextStart).coerceAtLeast(0)
        val isToday = daysAway == 0
        return NextClassEntry(
            state = if (isToday) NextClassState.Upcoming else NextClassState.DayDone,
            code = n.first.code,
            shortCode = shortCode(n.first.code),
            title = n.first.title,
            shortTitle = shortTitle(n.first.title),
            prof = n.first.prof.orEmpty(),
            room = n.first.room.orEmpty(),
            startsIn = startsIn,
            endsIn = 0,
            totalDurationMin = (nextEnd - nextStart).coerceAtLeast(1),
            startTime = n.first.startTime,
            endTime = n.first.endTime.orEmpty(),
            topic = n.first.topic,
            todayBars = bars,
            dayDoneLine = if (isToday) null else formatDayDoneLine(n, daysAway, weekdayResolver),
            completedTodayCount = completedCount,
            subjectColorArgb = SubjectPalette.argb(n.first.code, isDark),
        )
    }

    return NextClassEntry(
        state = NextClassState.DayDone,
        code = "",
        shortCode = "",
        title = "",
        shortTitle = "",
        prof = "",
        room = "",
        startsIn = 0,
        endsIn = 0,
        totalDurationMin = 0,
        startTime = "",
        endTime = "",
        topic = null,
        todayBars = bars,
        dayDoneLine = null,
        completedTodayCount = completedCount,
        subjectColorArgb = 0xFF3B9EAE.toInt(),
    )
}

// "amanhã, 07:30 · Cálculo I-E — PAT59" when the next class is the
// calendar day after now; otherwise substitutes the weekday name in pt-BR
// ("segunda, 07:30 …"). Mirrors `dayDoneLine` on iOS.
private fun formatDayDoneLine(
    nextDay: WidgetSnapshot.NextDay,
    daysAway: Int,
    weekdayResolver: (isoDay: String) -> String?,
): String {
    val when_ = when (daysAway) {
        0 -> "hoje"
        1 -> "amanhã"
        in 2..Int.MAX_VALUE -> weekdayResolver(nextDay.dateIso) ?: "em $daysAway dias"
        else -> "em breve"
    }
    val title = nextDay.first.title
    var line = "$when_, ${nextDay.first.startTime} · $title"
    val room = nextDay.first.room
    if (!room.isNullOrEmpty()) line += " — $room"
    return line
}

// Calendar-day distance between two `YYYY-MM-DD` strings, signed so a future
// `to` returns positive. Used to re-derive `daysAway` at render time from
// `nowDateIso` and the snapshot's `nextDay.dateIso` so the dayDone / upcoming
// branch picks the right state even when the snapshot is stale across a day
// boundary. Returns null if either input doesn't parse.
internal fun daysBetweenIsoDays(from: String, to: String): Int? {
    val a = parseIsoDay(from) ?: return null
    val b = parseIsoDay(to) ?: return null
    val diffMs = b.timeInMillis - a.timeInMillis
    // Use the per-day midpoint rounding to absorb DST transitions without
    // ever crossing a calendar-day boundary.
    return ((diffMs + (if (diffMs >= 0) 43_200_000L else -43_200_000L)) / 86_400_000L).toInt()
}

private fun parseIsoDay(iso: String): Calendar? {
    val parts = iso.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day)
    }
}

internal fun parseHhMm(value: String?): Int? {
    if (value.isNullOrBlank() || value.length < 4) return null
    val parts = value.split(":")
    if (parts.size < 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}

// Approximation of the design's `shortCode`. Upstream doesn't carry one, so
// we take the first whitespace-separated token of the discipline code, capped
// at 6 chars. Mirrors iOS's `shortCode`.
private fun shortCode(code: String): String {
    val first = code.split(" ").firstOrNull().orEmpty()
    return first.take(6)
}

// First clause before " — " / " – " / " - " / ":" if present, capped at 14
// chars. Upstream titles are full discipline names; the design shows a
// compact form. Mirrors iOS's `shortTitle`.
private fun shortTitle(title: String): String {
    val separators = listOf(" — ", " – ", " - ", ": ")
    var base = title
    for (sep in separators) {
        val idx = base.indexOf(sep)
        if (idx >= 0) base = base.substring(0, idx)
    }
    return if (base.length <= 14) base else base.take(13) + "…"
}

// 10-slot subject palette — same hex values + djb2 hashing as iOS
// `WidgetColor.subjectHex` and Android `ColorFor.discipline`. Carried here
// instead of consumed from the design system because Glance code is read
// outside `MaterialTheme` (which the design-system palette requires).
//
// Dark variants mirror `MelonPaletteColors` lifted-for-dark hexes
// (`apps/android/design-system/.../Color.kt`). The light slots are dim
// against the cool/dark plum mesh — particularly plum and indigo, which
// effectively disappear on dark surfaces — so we resolve to the lifted
// variant whenever the widget renders against the dark theme.
internal object SubjectPalette {
    private val light = longArrayOf(
        0xFFE85D4E, // coral
        0xFFF4A23C, // amber
        0xFFB23A7A, // magenta
        0xFF3B9EAE, // teal
        0xFF2D1B4E, // plum
        0xFFC64A6D, // rose
        0xFF3C7DC9, // sky
        0xFF2E8B5C, // emerald
        0xFF4A5FB8, // indigo
        0xFFA0741F, // mustard
    )

    private val dark = longArrayOf(
        0xFFF27E6E, // coral
        0xFFF4A23C, // amber
        0xFFD46299, // magenta
        0xFF5BB8C6, // teal
        0xFFB39DDB, // plum (lifted — was 0x2D1B4E, invisible on plum surface)
        0xFFE88AA5, // rose
        0xFF79AEE8, // sky
        0xFF5FC48E, // emerald
        0xFF8A9EE8, // indigo
        0xFFD4A84C, // mustard
    )

    fun argb(code: String, isDark: Boolean = false): Int {
        val palette = if (isDark) dark else light
        val bucket = (stableHash(code).let { if (it < 0) -it else it }) % palette.size
        return palette[bucket].toInt()
    }

    private fun stableHash(s: String): Int {
        var h = 5381
        for (c in s) h = ((h shl 5) + h) + c.code
        return h
    }
}

// Localized weekday name in pt-BR ("segunda", "terça", …). Used by the
// snapshot renderer when the next class is more than one day out so the
// dayDone copy reads naturally.
internal fun weekdayLabelForIsoDay(isoDay: String): String? = runCatching {
    val parts = isoDay.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toInt()
    val month = parts[1].toInt()
    val day = parts[2].toInt()
    val cal = java.util.Calendar.getInstance(Locale.Builder().setLanguage("pt").setRegion("BR").build())
    cal.set(year, month - 1, day)
    val df = java.text.SimpleDateFormat("EEEE", Locale.Builder().setLanguage("pt").setRegion("BR").build())
    df.format(cal.time)
        .replace("-feira", "")
        .lowercase(Locale.Builder().setLanguage("pt").setRegion("BR").build())
}.getOrNull()
