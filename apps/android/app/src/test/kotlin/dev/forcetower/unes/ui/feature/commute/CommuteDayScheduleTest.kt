package dev.forcetower.unes.ui.feature.commute

import dev.forcetower.melon.feature.transit.domain.model.CommuteLeg
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Which leg the Hoje line asks about: in until the first class, home after.
internal class CommuteDayScheduleTest {
    private val toCampus = CommuteLeg("3203", "Av. Maria Quitéria, 3203", "Queimadinha", listOf("087"), 5)
    private val fromCampus = CommuteLeg("2452", "Av. Transnordestina", "Novo Horizonte (Saída)", listOf("087"), 3)
    private val both = CommutePlan(toCampus = toCampus, fromCampus = fromCampus)

    @Test
    fun firstClassSplitsTheDay() {
        val schedule = CommuteDaySchedule.fromStartTimes(listOf("10:00", "07:30", "14:00"))
        assertEquals(TransitDirection.ToCampus, schedule.direction(both, LocalTime.of(6, 50)))
        assertEquals(TransitDirection.FromCampus, schedule.direction(both, LocalTime.of(7, 30)))
        assertEquals(TransitDirection.FromCampus, schedule.direction(both, LocalTime.of(20, 48)))
    }

    @Test
    fun aDayWithoutClassesSplitsAtNoon() {
        val schedule = CommuteDaySchedule()
        assertEquals(TransitDirection.ToCampus, schedule.direction(both, LocalTime.of(11, 59)))
        assertEquals(TransitDirection.FromCampus, schedule.direction(both, LocalTime.of(12, 0)))
    }

    @Test
    fun aSingleLegPlanAlwaysAnswersThatLeg() {
        val schedule = CommuteDaySchedule(firstStartMinute = 7 * 60)
        assertEquals(
            TransitDirection.FromCampus,
            schedule.direction(CommutePlan(fromCampus = fromCampus), LocalTime.of(6, 0)),
        )
        assertEquals(
            TransitDirection.ToCampus,
            schedule.direction(CommutePlan(toCampus = toCampus), LocalTime.of(22, 0)),
        )
        assertNull(schedule.direction(CommutePlan(), LocalTime.of(9, 0)))
    }

    @Test
    fun unparsableStartTimesAreIgnored() {
        assertEquals(8 * 60, CommuteDaySchedule.fromStartTimes(listOf("--", "08:00:00", "x:y")).firstStartMinute)
        assertNull(CommuteDaySchedule.fromStartTimes(emptyList()).firstStartMinute)
    }
}
