package dev.forcetower.unes.ui.feature.commute

import dev.forcetower.melon.feature.transit.domain.model.CommuteLeg
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitArrivalState
import dev.forcetower.melon.feature.transit.domain.model.TransitVehiclePhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// The estimate rules every commute surface shares: a minute band on the
// way, a clock band while waiting or when the signal is gone, an approximate
// time from history or far out, and the "frozen" board that must not count.
internal class CommuteFormatTest {
    private val now = Instant.parse("2026-10-02T09:50:00Z")

    @Test
    fun liveBusOnItsWayGetsAMinuteBand() {
        val arrival = arrival(TransitArrivalState.Live, earliest = 8.minutes, latest = 10.minutes)
        assertEquals(CommuteEstimate.MinuteRange(8, 10), CommuteFormat.estimate(arrival, now))
    }

    @Test
    fun bandCollapsesToOneNumberWhenBothEndsRoundTogether() {
        val arrival = arrival(TransitArrivalState.Live, earliest = 5.minutes, latest = 5.minutes)
        assertEquals(CommuteEstimate.Minutes(5), CommuteFormat.estimate(arrival, now))
        val wide = arrival(TransitArrivalState.Live, earliest = 4.minutes + 50.seconds, latest = 5.minutes)
        assertEquals(CommuteEstimate.MinuteRange(4, 5), CommuteFormat.estimate(wide, now))
    }

    @Test
    fun aBusAboutToArriveSaysSoInsteadOfZero() {
        val arrival = arrival(TransitArrivalState.Live, earliest = 10.seconds, latest = 50.seconds)
        assertEquals(CommuteEstimate.Arriving, CommuteFormat.estimate(arrival, now))
    }

    @Test
    fun aBusAtTheStopSaysSo() {
        val arrival = arrival(TransitArrivalState.Live, earliest = 0.minutes, latest = 2.minutes)
            .copy(phase = TransitVehiclePhase.AtStop)
        assertEquals(CommuteEstimate.AtStop, CommuteFormat.estimate(arrival, now))
    }

    @Test
    fun waitingSignalLostAndFrozenKeepTheClockBand() {
        val waiting = arrival(TransitArrivalState.Waiting, earliest = 12.minutes, latest = 18.minutes)
        val lost = arrival(TransitArrivalState.Live, earliest = 8.minutes, latest = 10.minutes).copy(signalLost = true)
        val live = arrival(TransitArrivalState.Live, earliest = 8.minutes, latest = 10.minutes)
        assertEquals(CommuteEstimate.ClockRange(waiting.earliest, waiting.latest), CommuteFormat.estimate(waiting, now))
        assertEquals(CommuteEstimate.ClockRange(lost.earliest, lost.latest), CommuteFormat.estimate(lost, now))
        assertEquals(
            CommuteEstimate.ClockRange(live.earliest, live.latest),
            CommuteFormat.estimate(live, now, frozen = true),
        )
    }

    @Test
    fun historyAndFarAwayBusesOnlyGetAnApproximateTime() {
        val historical = arrival(TransitArrivalState.Historical, earliest = 27.minutes, latest = 33.minutes)
        val far = arrival(TransitArrivalState.Live, earliest = 40.minutes, latest = 44.minutes)
        assertEquals(
            CommuteEstimate.Clock(historical.expected, isApproximate = true),
            CommuteFormat.estimate(historical, now),
        )
        assertEquals(CommuteEstimate.Clock(far.expected, isApproximate = true), CommuteFormat.estimate(far, now))
    }

    @Test
    fun aBoardOlderThanAMinuteIsOutdated() {
        assertFalse(CommuteFormat.isOutdated(now - 59.seconds, now))
        assertTrue(CommuteFormat.isOutdated(now - 61.seconds, now))
        assertFalse(CommuteFormat.isOutdated(null, now))
    }

    @Test
    fun departureStopNameDropsTheProviderSuffix() {
        val leg = CommuteLeg("2452", "Av. Transnordestina", "Novo Horizonte (Saída)", listOf("087"), 3)
        assertEquals("Av. Transnordestina · Novo Horizonte", CommuteFormat.departureStopName(leg))
    }

    @Test
    fun flakyNeedsThreeObservedDaysAndUnderEightyPercent() {
        assertTrue(arrival(TransitArrivalState.Historical).copy(ranDays = 6, observedDays = 10).isFlaky)
        assertFalse(arrival(TransitArrivalState.Historical).copy(ranDays = 8, observedDays = 10).isFlaky)
        assertFalse(arrival(TransitArrivalState.Historical).copy(ranDays = 1, observedDays = 2).isFlaky)
    }

    private fun arrival(
        state: TransitArrivalState,
        earliest: kotlin.time.Duration = 5.minutes,
        latest: kotlin.time.Duration = 7.minutes,
    ) = TransitArrival(
        id = "087-1",
        lineNumber = "087",
        state = state,
        phase = if (state == TransitArrivalState.Live) TransitVehiclePhase.EnRoute else null,
        earliest = now + earliest,
        latest = now + latest,
        expected = now + (earliest + latest) / 2,
    )
}
