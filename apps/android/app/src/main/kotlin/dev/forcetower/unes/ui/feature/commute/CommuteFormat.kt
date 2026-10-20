package dev.forcetower.unes.ui.feature.commute

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.forcetower.melon.feature.transit.domain.model.CommuteLeg
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitArrivalState
import dev.forcetower.melon.feature.transit.domain.model.TransitVehiclePhase
import dev.forcetower.unes.R
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// The number that matters: a minute band while the bus is on its way, a
// clock band while it waits at its origin, and only an approximate time when
// there is no bus or it is more than 30 minutes out. Never seconds. The
// rendering lives in `CommuteComponents`; this is the decision.
internal sealed interface CommuteEstimate {
    // Rendered with a smaller, dimmer leading "~".
    val isApproximate: Boolean get() = false

    data class Clock(
        val time: Instant,
        override val isApproximate: Boolean,
    ) : CommuteEstimate

    data class ClockRange(
        val start: Instant,
        val end: Instant,
    ) : CommuteEstimate

    data object AtStop : CommuteEstimate

    data object Arriving : CommuteEstimate

    data class Minutes(
        val minutes: Int,
    ) : CommuteEstimate

    data class MinuteRange(
        val low: Int,
        val high: Int,
    ) : CommuteEstimate
}

// The display rules for a bus estimate and the clock helpers every commute
// screen shares (iOS `CommuteFormat`).
internal object CommuteFormat {
    // A board older than this on the device is shown frozen: the connection
    // is gone, so nothing may keep counting down as if it were live.
    val outdatedAfter: Duration = 60.seconds

    private val approximateBeyond: Duration = 30.minutes

    // `frozen` is for data the device could no longer refresh.
    fun estimate(
        arrival: TransitArrival,
        now: Instant,
        frozen: Boolean = false,
    ): CommuteEstimate {
        if (arrival.state == TransitArrivalState.Historical || arrival.expected - now > approximateBeyond) {
            return CommuteEstimate.Clock(arrival.expected, isApproximate = true)
        }
        // A bus that stopped reporting (or a board we can't refresh) keeps its
        // last band on the clock, so it stops counting down instead of pretending to move.
        if (arrival.state == TransitArrivalState.Waiting || arrival.signalLost || frozen) {
            return CommuteEstimate.ClockRange(arrival.earliest, arrival.latest)
        }
        if (arrival.phase == TransitVehiclePhase.AtStop) return CommuteEstimate.AtStop
        val low = floor((arrival.earliest - now).inWholeSeconds / 60.0).toInt()
        val high = ceil((arrival.latest - now).inWholeSeconds / 60.0).toInt()
        if (high <= 1) return CommuteEstimate.Arriving
        val floorLow = max(1, low)
        return if (floorLow >= high) CommuteEstimate.Minutes(high) else CommuteEstimate.MinuteRange(floorLow, high)
    }

    fun isOutdated(
        receivedAt: Instant?,
        now: Instant,
    ): Boolean = receivedAt != null && now - receivedAt > outdatedAfter

    fun minutesUntil(
        date: Instant,
        now: Instant,
    ): Int = max(1, ((date - now).inWholeSeconds / 60.0).roundToInt())

    // "Av. Transnordestina · Novo Horizonte": the provider marks the campus
    // departure stop "(Saída)", which the screens already say in words.
    fun departureStopName(leg: CommuteLeg): String =
        listOfNotNull(leg.stopName, leg.stopArea)
            .map { it.replace(" (Saída)", "") }
            .joinToString(" · ")

    fun local(
        instant: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LocalDateTime = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()).atZone(zone).toLocalDateTime()

    // "07:02" — the same shape the server uses for official departures.
    fun clock(
        instant: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = ClockFormatter.format(local(instant, zone))

    private val ClockFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // "segunda-feira" — lowercase full weekday for "Próxima viagem · segunda".
    fun weekdayName(
        instant: Instant,
        locale: Locale = Locale.getDefault(),
    ): String = DateTimeFormatter.ofPattern("EEEE", locale).format(local(instant)).lowercase(locale)

    // "400 m" / "1,9 km" — distance to a stop from the student's fix.
    fun distanceParts(meters: Double): Pair<Int?, String?> =
        if (meters < 1_000) {
            meters.roundToInt() to null
        } else {
            null to String.format(Locale.getDefault(), "%.1f", meters / 1_000)
        }
}

// ───────── composable labels ─────────

// Ticks every second while the caller is composed, the Android analogue of
// the iOS `TimelineView(.periodic(by: 1))` every commute screen wraps itself in.
@Composable
internal fun rememberCommuteNow(): Instant {
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1_000)
            now = Clock.System.now()
        }
    }
    return now
}

// "07:02" — never seconds.
@Composable
internal fun clockLabel(instant: Instant): String = CommuteFormat.clock(instant)

// "07:02–07:08", collapsing to one time when both fall in the same minute.
@Composable
internal fun clockRangeLabel(
    start: Instant,
    end: Instant,
): String {
    val from = clockLabel(start)
    val to = clockLabel(end)
    return if (from == to) from else from + stringResource(R.string.commute_range_separator) + to
}

@Composable
internal fun CommuteEstimate.label(): String =
    when (this) {
        is CommuteEstimate.Clock -> clockLabel(time)
        is CommuteEstimate.ClockRange -> clockRangeLabel(start, end)
        CommuteEstimate.AtStop -> stringResource(R.string.commute_estimate_at_stop)
        CommuteEstimate.Arriving -> stringResource(R.string.commute_estimate_arriving)
        is CommuteEstimate.Minutes -> stringResource(R.string.commute_estimate_minutes, minutes)
        is CommuteEstimate.MinuteRange -> stringResource(R.string.commute_estimate_minute_range, low, high)
    }

// "atualizado há 8 s" — the bus's last new position, not our request.
@Composable
internal fun freshnessLabel(
    fixAt: Instant,
    now: Instant,
    short: Boolean = false,
): String {
    val seconds = max(0, (now - fixAt).inWholeSeconds).toInt()
    return when {
        seconds < 60 && short -> stringResource(R.string.commute_fresh_seconds_short, seconds)
        seconds < 60 -> stringResource(R.string.commute_fresh_seconds, seconds)
        short -> stringResource(R.string.commute_fresh_minutes_short, seconds / 60)
        else -> stringResource(R.string.commute_fresh_minutes, seconds / 60)
    }
}

@Composable
internal fun signalLostLabel(
    fixAt: Instant,
    now: Instant,
): String = stringResource(R.string.commute_signal_lost_for, max(1, (now - fixAt).inWholeMinutes).toInt())

// "087 e 088" — natural-language list of line numbers.
@Composable
internal fun lineListLabel(numbers: List<String>): String {
    if (numbers.size < 2) return numbers.joinToString("")
    val separator = stringResource(R.string.commute_list_separator)
    val last = stringResource(R.string.commute_list_last_separator)
    return numbers.dropLast(1).joinToString(separator) + last + numbers.last()
}

@Composable
internal fun stopsAwayLabel(stops: Int): String =
    if (stops == 1) {
        stringResource(R.string.commute_next_stop)
    } else {
        pluralStringResource(R.plurals.commute_stops_away, stops, stops)
    }

@Composable
internal fun distanceLabel(meters: Double): String {
    val (wholeMeters, kilometers) = CommuteFormat.distanceParts(meters)
    return if (wholeMeters != null) {
        stringResource(R.string.commute_distance_meters, wholeMeters)
    } else {
        stringResource(R.string.commute_distance_kilometers, kilometers.orEmpty())
    }
}
