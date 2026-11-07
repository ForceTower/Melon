package dev.forcetower.unes.commute

import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitRoute
import dev.forcetower.unes.ui.feature.commute.CommuteEstimate
import dev.forcetower.unes.ui.feature.commute.CommuteFixtures
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

// What the followed-bus notification says and how it draws the route line,
// decided before any string or platform style gets involved.
internal class CommuteLiveUpdateTest {
    private val now = Instant.parse("2026-10-02T09:50:00Z")
    private val inbound = CommuteFixtures.network.route("087", TransitDirection.ToCampus)!!
    private val follow = CommuteFixtures.follow(now)
    private val busProgress = follow.vehicles[0].progressMeters

    private fun record(
        arrival: TransitArrival = follow.arrival!!,
        route: TransitRoute = inbound,
        stopId: String = CommuteFixtures.MY_STOP_ID,
        progressMeters: Double? = busProgress,
    ) = CommuteLiveRecord.of("activity", arrival, route, "Expresso Universitário", stopId, progressMeters, now)!!

    @Test
    fun aBusOnItsWayCountsDownToTheStudentsStop() {
        val view = assertIs<CommuteLiveView.Following>(commuteLiveView(record(), now, stale = false))
        assertEquals(CommuteEstimate.MinuteRange(8, 10), view.estimate)
        assertEquals(view.estimate, view.chip)
        assertFalse(view.untilDeparture)
        assertNull(view.degraded)
        assertEquals(4, view.stopsAway)
        assertEquals(now + 22.minutes, view.campusArrival)
    }

    @Test
    fun noPushInTimeFreezesTheBandOnTheClock() {
        val view = assertIs<CommuteLiveView.Following>(commuteLiveView(record(), now, stale = true))
        assertIs<CommuteEstimate.ClockRange>(view.estimate)
        assertEquals(CommuteEstimate.Clock(now + 9.minutes, isApproximate = true), view.chip)
        assertEquals(CommuteLiveView.Degraded.Offline, view.degraded)
    }

    @Test
    fun aBusThatLostItsSignalStopsCountingAndSaysSo() {
        val lost = record().let { it.copy(update = it.update.copy(signalLost = true)) }
        val view = assertIs<CommuteLiveView.Following>(commuteLiveView(lost, now, stale = false))
        assertIs<CommuteEstimate.ClockRange>(view.estimate)
        assertEquals(CommuteLiveView.Degraded.SignalLost, view.degraded)
    }

    @Test
    fun anEndedActivitySaysWhy() {
        val passed = record().copy(update = CommuteLiveRecord.Update(status = "passed", progressMeters = 9_999.0))
        assertEquals(CommuteLiveView.Passed, commuteLiveView(passed, now, stale = false))
        val ended = record().copy(update = CommuteLiveRecord.Update(status = "ended"))
        assertEquals(CommuteLiveView.NoEstimate, commuteLiveView(ended, now, stale = false))
    }

    @Test
    fun theRouteLineRunsToTheStudentsStopWithTheBusOnIt() {
        val record = record()
        val track = commuteLiveTrack(record)
        val mine = inbound.indexOf(CommuteFixtures.MY_STOP_ID)!!
        assertEquals(mine, track.segments.size)
        assertTrue(track.mineAtEnd)
        assertNull(track.minePoint)
        assertEquals((busProgress!! - inbound.stops[0].distanceMeters).roundToInt(), track.progress)
        assertNull(commuteLiveTrack(record(progressMeters = null)).progress)
    }

    @Test
    fun leavingCampusFromWhereTheLineStartsWaitsForTheDeparture() {
        val departure = CommuteFixtures.board(now, TransitDirection.FromCampus).arrivals[0]
        val outbound = CommuteFixtures.network.route(departure.lineNumber, TransitDirection.FromCampus)!!
        val record = record(departure, outbound, CommuteFixtures.CAMPUS_DEPARTURE_STOP_ID, progressMeters = null)

        val view = assertIs<CommuteLiveView.Following>(commuteLiveView(record, now, stale = false))
        assertTrue(view.untilDeparture)
        val track = commuteLiveTrack(record)
        assertTrue(track.mineAtStart)
        assertEquals(outbound.stops.size - 1, track.segments.size)
    }

    @Test
    fun aStoredUpdateReadsBackAsTheSameEstimate() {
        val record = record()
        val arrival = record.update.toDomain().arrival(record.arrivalId, record.lineNumber)!!
        assertEquals(follow.arrival!!.earliest, arrival.earliest)
        assertEquals(follow.arrival!!.campusArrival, arrival.campusArrival)
        assertEquals(busProgress, record.update.progressMeters)
    }

    @Test
    fun aTapOpensTheFollowScreenOnTheNotificationsOwnUpdate() {
        val record = record().copy(vehicleId = "90001")
        val seed = assertNotNull(record.followSeed())
        assertEquals(record.arrivalId, seed.arrival.id)
        assertEquals(follow.arrival!!.earliest, seed.arrival.earliest)
        assertEquals("90001", seed.arrival.vehicleId)
        assertEquals(TransitDirection.ToCampus, seed.direction)
        assertEquals(CommuteFixtures.MY_STOP_ID, seed.stopId)
        assertEquals(now, seed.receivedAt)
    }

    @Test
    fun aBusThatPassedLeavesNothingToFollow() {
        val passed = record().copy(update = CommuteLiveRecord.Update(status = "passed"))
        assertNull(passed.followSeed())
    }

    @Test
    fun aFollowExpiresEightHoursAfterItStartedWhateverPushesCameSince() {
        val pushed = record().copy(updatedAt = (now + 30.minutes).epochSeconds)
        val stored = json.decodeFromString<CommuteLiveRecord>(json.encodeToString(pushed))
        assertEquals(now + 8.hours, stored.expiry)
        assertFalse(stored.expired(now + 8.hours - 1.seconds))
        assertTrue(stored.expired(now + 8.hours))
    }

    @Test
    fun aFollowStoredBeforeItHadAnExpiryEndsEightHoursAfterItsLastUpdate() {
        val lastUpdate = now + 30.minutes
        val current = json.encodeToJsonElement(record().copy(updatedAt = lastUpdate.epochSeconds)).jsonObject
        val legacy = JsonObject(current - "expiresAt" + ("registered" to JsonPrimitive(true)))
        val stored = json.decodeFromJsonElement<CommuteLiveRecord>(legacy)
        assertEquals(lastUpdate + 8.hours, stored.expiry)
        assertNull(stored.registeredToken)
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
