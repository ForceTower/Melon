package dev.forcetower.unes.ui.feature.enrollment

import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentDiscipline
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentMeeting
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSection
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentShift
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSlot
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindow
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindowState

internal object EnrollmentTestFixtures {
    fun window() =
        EnrollmentWindow(
            semester = "2026.2",
            state = EnrollmentWindowState.Open,
            startDate = "2026-10-01T00:00:00-03:00",
            endDate = "2026-10-03T00:00:00-03:00",
            minHours = 60,
            maxHours = 240,
            useQueue = true,
            courseId = 901,
        )

    fun discipline(
        id: Long,
        vararg sections: EnrollmentSection,
    ) = EnrollmentDiscipline(
        id = id,
        code = "SYN$id",
        name = "Disciplina sintética $id",
        workload = 60,
        mandatory = true,
        gradePeriod = 1,
        suggestion = false,
        prerequisites = emptyList(),
        sections = sections.toList(),
    )

    fun section(
        id: Long,
        vararg slots: EnrollmentSlot,
    ) = EnrollmentSection(
        id = id,
        label = "T$id",
        coursePreferential = false,
        suggestion = false,
        vacancies = 20,
        proposalsCount = 0,
        allowsOtherDefault = false,
        waitlistCount = 0,
        selected = false,
        meetings = if (slots.isEmpty()) emptyList() else listOf(meeting(*slots)),
    )

    fun meeting(vararg slots: EnrollmentSlot) =
        EnrollmentMeeting(
            kind = "THEORY",
            shift = EnrollmentShift.Morning,
            professors = emptyList(),
            room = null,
            slots = slots.toList(),
        )
}
