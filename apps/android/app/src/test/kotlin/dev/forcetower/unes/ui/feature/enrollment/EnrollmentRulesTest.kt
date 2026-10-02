package dev.forcetower.unes.ui.feature.enrollment

import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentDiscipline
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentPrerequisite
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSelection
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSlot
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindow
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindowState
import dev.forcetower.unes.ui.feature.enrollment.EnrollmentTestFixtures.discipline
import dev.forcetower.unes.ui.feature.enrollment.EnrollmentTestFixtures.meeting
import dev.forcetower.unes.ui.feature.enrollment.EnrollmentTestFixtures.section
import dev.forcetower.unes.ui.feature.enrollment.EnrollmentTestFixtures.window
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class EnrollmentRulesTest {
    @Test
    fun overlappingSlotsConflictOnlyOnTheSameWeekday() {
        val first = EnrollmentSlot(1, "08:00", "10:00")
        assertTrue(slotsOverlap(first, EnrollmentSlot(1, "09:59", "12:00")))
        assertTrue(slotsOverlap(first, EnrollmentSlot(1, "08:30:00", "09:00:00")))
        assertFalse(slotsOverlap(first, EnrollmentSlot(2, "08:00", "10:00")))
    }

    @Test
    fun adjacentSectionsDoNotConflictInEitherOrder() {
        val first = EnrollmentSlot(1, "08:00", "10:00")
        val second = EnrollmentSlot(1, "10:00", "12:00")
        assertFalse(slotsOverlap(first, second))
        assertFalse(slotsOverlap(second, first))
    }

    @Test
    fun conflictsIncludeLaterMeetingsAndIgnoreUnscheduledSections() {
        val first = section(11).copy(
            meetings = listOf(
                meeting(EnrollmentSlot(1, "08:00", "10:00")),
                meeting(EnrollmentSlot(3, "13:00", "15:00")),
            ),
        )
        val second = section(21, EnrollmentSlot(3, "14:00", "16:00"))
        assertEquals(3, conflictDay(first, second))
        assertNull(conflictDay(first, section(31)))
        assertFalse(section(31).hasSchedule)
    }

    @Test
    fun eachConflictingPairBlocksOnceEvenWhenItOverlapsOnTwoDays() {
        val slots = arrayOf(EnrollmentSlot(1, "08:00", "10:00"), EnrollmentSlot(3, "08:00", "10:00"))
        val state = state(
            listOf(
                discipline(1, section(11, *slots)),
                discipline(2, section(21, *slots)),
                discipline(3, section(31, *slots)),
            ),
        )

        assertEquals(3, state.conflicts.size)
        assertEquals(listOf(EnrollmentBlocker.Conflicts(3)), state.blockers)
        assertFalse(state.canSubmit)
    }

    @Test
    fun replacingASectionWithinOneDisciplineDoesNotConflictWithItself() {
        val original = section(11, EnrollmentSlot(1, "08:00", "10:00"))
        val alternative = section(12, EnrollmentSlot(1, "09:00", "11:00"))
        val discipline = discipline(1, original, alternative)
        val state = state(listOf(discipline))

        assertNull(state.clashFor(discipline, alternative))
        val other = discipline(2, section(21, EnrollmentSlot(1, "09:00", "11:00")))
        assertEquals(1L, state.clashFor(other, other.sections.single())?.discipline?.id)
    }

    @Test
    fun hourLimitsAllowExactBoundsAndBlockOneHourOutside() {
        val window = window().copy(minHours = 60, maxHours = 180)
        val cases = listOf(
            59 to listOf(EnrollmentBlocker.UnderMinimum(1)),
            60 to emptyList(),
            180 to emptyList(),
            181 to listOf(EnrollmentBlocker.OverMaximum(1)),
        )
        for ((hours, expected) in cases) {
            val state = state(listOf(discipline(1, section(11)).copy(workload = hours)), window)
            assertEquals(expected, state.blockers, "workload=$hours")
            assertEquals(expected.isEmpty(), state.canSubmit, "workload=$hours")
        }
    }

    @Test
    fun workloadCountsADisciplineOnceRegardlessOfMeetingCount() {
        val section = section(11).copy(
            meetings = listOf(
                meeting(EnrollmentSlot(1, "08:00", "10:00")),
                meeting(EnrollmentSlot(3, "08:00", "10:00")),
            ),
        )
        assertEquals(60, state(listOf(discipline(1, section))).totalHours)
    }

    @Test
    fun emptyProposalCannotSubmitEvenWithoutAMinimum() {
        val state = state(emptyList(), window().copy(minHours = 0))
        assertEquals(listOf(EnrollmentBlocker.Empty), state.blockers)
        assertFalse(state.canSubmit)
    }

    @Test
    fun submissionIsBlockedAfterTheDeadlineEvenWhenPortalStillSaysOpen() {
        val state = state(listOf(discipline(1, section(11)))).copy(
            referenceNowMillis = Instant.parse("2026-10-03T03:00:00.001Z").toEpochMilli(),
        )
        assertFalse(state.canSubmit)
        assertEquals(listOf(EnrollmentBlocker.DeadlinePassed), state.blockers)
    }

    @Test
    fun submissionAtTheDeadlineIsStillAllowed() {
        val state = state(listOf(discipline(1, section(11)))).copy(
            referenceNowMillis = Instant.parse("2026-10-03T03:00:00Z").toEpochMilli(),
        )
        assertTrue(state.canSubmit)
    }

    @Test
    fun officialOpeningInTheFutureDoesNotBlockAnAlreadyOpenPortal() {
        val state = state(
            listOf(discipline(1, section(11))),
            window().copy(startDate = "2026-10-02T18:00:00-03:00"),
        )
        assertTrue(state.canSubmit)
    }

    @Test
    fun closedProposalMustBeReopenedAndUpcomingOrUnknownCannotSubmit() {
        val state = state(listOf(discipline(1, section(11))))
        val closed = state.copy(window = window().copy(state = EnrollmentWindowState.Closed))
        assertTrue(closed.isReadonly)
        assertFalse(closed.canSubmit)
        assertTrue(closed.copy(reopened = true).canSubmit)
        for (status in listOf(EnrollmentWindowState.Upcoming, EnrollmentWindowState.Unknown)) {
            assertFalse(state.copy(window = window().copy(state = status), reopened = true).canSubmit)
        }
        assertFalse(state.copy(window = null).canSubmit)
        assertFalse(state.copy(submitting = true).canSubmit)
    }

    @Test
    fun reopeningDoesNotAllowSubmissionAfterTheDeadline() {
        val state = state(
            listOf(discipline(1, section(11))),
            window().copy(state = EnrollmentWindowState.Closed),
        ).copy(reopened = true, referenceNowMillis = Instant.parse("2026-10-04T00:00:00Z").toEpochMilli())
        assertFalse(state.canSubmit)
    }

    @Test
    fun unmetPrerequisitesWarnWithoutBlockingAnOfferedDiscipline() {
        val discipline = discipline(1, section(11)).copy(
            prerequisites = listOf(EnrollmentPrerequisite("SYN100", "Fundamentos sintéticos", false)),
        )
        val state = state(listOf(discipline))
        assertTrue(discipline.hasUnmetPrerequisite)
        assertTrue(state.blockers.isEmpty())
        assertTrue(state.canSubmit)
        assertFalse(
            discipline.copy(prerequisites = discipline.prerequisites.map { it.copy(met = true) })
                .hasUnmetPrerequisite,
        )
    }

    @Test
    fun fullSectionsJoinWaitlistOnlyWhenTheWindowUsesAQueue() {
        val full = section(11).copy(vacancies = 10, proposalsCount = 10)
        val discipline = discipline(1, full)
        assertTrue(makePick(window().copy(useQueue = true), discipline, full).waitlist)
        assertFalse(makePick(window().copy(useQueue = false), discipline, full).waitlist)
        assertFalse(makePick(null, discipline, full).waitlist)
        assertFalse(makePick(window(), discipline, full.copy(proposalsCount = 9)).waitlist)
        assertTrue(makePick(window(), discipline, full.copy(proposalsCount = 11)).waitlist)
        assertTrue(makePick(window(), discipline, full.copy(vacancies = 0, proposalsCount = 0)).waitlist)
    }

    @Test
    fun savedProposalSeedsOnlySelectedSectionsAndRestoresSectionDefaults() {
        val first = discipline(1, section(11), section(12).copy(selected = true, allowsOtherDefault = true))
        val second = discipline(2, section(21).copy(selected = true, vacancies = 0))
        val third = discipline(3, section(31))
        assertEquals(
            listOf(EnrollmentPick(1, 12, true, false), EnrollmentPick(2, 21, false, true)),
            preseedPicks(window(), listOf(first, second, third)),
        )
    }

    @Test
    fun editedProposalContainsTheEntireDesiredSetAndOmitsRemovedOrReplacedSections() {
        val catalogue = listOf(
            discipline(1, section(11).copy(selected = true), section(12)),
            discipline(2, section(21).copy(selected = true)),
            discipline(3, section(31).copy(selected = true)),
            discipline(4, section(41)),
        )
        val state = state(catalogue).copy(
            picks = listOf(
                EnrollmentPick(1, 12, true, false),
                EnrollmentPick(3, 31, false, false),
                EnrollmentPick(4, 41, false, true),
            ),
        )
        assertEquals(
            listOf(
                EnrollmentSelection(12, true, false),
                EnrollmentSelection(31, false, false),
                EnrollmentSelection(41, false, true),
            ),
            state.selections,
        )
        assertEquals(180, state.totalHours)
        assertEquals(1, state.waitlistedCount)
        assertEquals(1, state.allowsOtherCount)
    }

    @Test
    fun vanishedCatalogueRowsCannotLeakIntoTheSubmitPayloadOrWorkload() {
        val state = state(listOf(discipline(1, section(11)), discipline(2, section(21))))
            .copy(disciplines = listOf(discipline(1, section(12)), discipline(2, section(21))))
        assertEquals(listOf(EnrollmentSelection(21, false, false)), state.selections)
        assertEquals(60, state.totalHours)
    }

    private fun state(
        disciplines: List<EnrollmentDiscipline>,
        window: EnrollmentWindow = window(),
    ) = EnrollmentUiState(
        phase = EnrollmentPhase.Loaded,
        available = true,
        window = window,
        disciplines = disciplines,
        picks = disciplines.map { makePick(window, it, it.sections.first()) },
        referenceNowMillis = Instant.parse("2026-10-02T12:00:00Z").toEpochMilli(),
    )
}
