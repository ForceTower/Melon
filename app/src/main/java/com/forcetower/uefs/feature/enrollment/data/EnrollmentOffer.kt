package com.forcetower.uefs.feature.enrollment.data

data class EnrollmentOffer(
    val id: Long,
    val mandatory: Boolean,
    val gradePeriod: Int,
    val activity: CurricularActivitySimple,
    val prequisites: List<Prerequisite>,
    val classes: List<ClassOfferItem>
) {
    data class CurricularActivitySimple(
        val id: Long,
        val code: String,
        val name: String,
        val workload: Int,
    )

    data class Prerequisite(
        val id: Long,
        val code: String,
        val name: String,
        val syllabus: String?,
    )

    /**
     * Wrapper for a Class offer.
     * This is essentially a T01
     */
    data class ClassOfferItem(
        val id: Long,
        val coursePreferential: Boolean,
        val suggestion: Boolean,
        val enrollmentConfirmed: Boolean,
        val proposalSaved: Boolean,
        val allowsOtherClassEnrollment: Boolean,
        val vacancies: Int,
        val proposalsCount: Int,
        val sharesVacancies: Boolean,
        // I feel like this is going to be useful when the enrollment period is over
        val waitlistParticipant: Boolean,
        val waitlistPosition: Int,
        val waitlistStudentsCount: Int,
        val classGroup: ClassGroup
    )

    data class ClassGroup(
        /**
         * The actual important bit
         */
        val id: Long,
        /**
         * T01P01
         */
        val description: String,
        /**
         * The groups for this class.
         * One could be T01 and the other T01P01
         */
        val classes: List<ClassDetail>
    )

    data class ClassDetail(
        val id: Long,
        val description: String,
        val type: String,
        val fixedAllocation: Boolean,
        val shift: String,
        val studentCount: Int,
        val lessonCount: Int,
        val professors: List<Person>,
        val curricularActivity: CurricularActivityFull,
        val allocations: List<Allocation>,
    )

    data class CurricularActivityFull(
        val id: Long,
        val code: String,
        val name: String,
        val syllabus: String?,
        val bibliography: String?,
        val workload: Int
    )

    data class Allocation(
        val id: Long,
        val schedule: Schedule,
        val physicalSpace: PhysicalSpace?
    )

    data class Schedule(
        val id: Long,
        val day: Int,
        val start: String,
        val end: String
    )

    data class PhysicalSpace(
        val id: Long,
        /**
         * Classroom , lol
         */
        val type: String,
        /**
         * UEFS most of the time, idk
         */
        val pavilion: String?,
        /**
         * The actual class
         * PAT47
         */
        val number: String?
    )
}

data class Person(
    val id: Long,
    val name: String,
    val displayName: String?,
    val personKind: String?,
    val cpf: String?,
    val email: String?
)
