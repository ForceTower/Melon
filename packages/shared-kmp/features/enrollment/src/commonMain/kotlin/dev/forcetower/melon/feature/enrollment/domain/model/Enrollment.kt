package dev.forcetower.melon.feature.enrollment.domain.model

// Domain models consumed by the native enrollment flow. The iOS ViewModel maps
// these into its presentation structs (deriving colors, seat meters, schedule
// conflicts and labels on-device).

data class EnrollmentAvailability(
    val available: Boolean,
    val window: EnrollmentWindow?,
)

// Wraps the disciplines list in a concrete type: a bare `List<T>` returned
// inside `Outcome<List<T>>` erases to `NSArray?` across the ObjC boundary, but a
// `List` field on a concrete class bridges to a typed Swift array.
data class EnrollmentOffers(
    val disciplines: List<EnrollmentDiscipline>,
)

data class EnrollmentWindow(
    val semester: String,
    val state: EnrollmentWindowState,
    val startDate: String,
    val endDate: String,
    val minHours: Int,
    val maxHours: Int,
    val useQueue: Boolean,
    val courseId: Long,
)

enum class EnrollmentWindowState { Open, Upcoming, Closed, Unknown }

data class EnrollmentDiscipline(
    val id: Long,
    val code: String,
    val name: String,
    val workload: Int,
    val mandatory: Boolean,
    val gradePeriod: Int,
    val suggestion: Boolean,
    val prerequisites: List<EnrollmentPrerequisite>,
    val sections: List<EnrollmentSection>,
)

data class EnrollmentPrerequisite(
    val code: String,
    val name: String,
    val met: Boolean,
)

data class EnrollmentSection(
    val id: Long,
    val label: String,
    val coursePreferential: Boolean,
    val suggestion: Boolean,
    val vacancies: Int,
    val proposalsCount: Int,
    val allowsOtherDefault: Boolean,
    val waitlistCount: Int,
    val selected: Boolean,
    val meetings: List<EnrollmentMeeting>,
)

data class EnrollmentMeeting(
    val kind: String,
    val shift: EnrollmentShift,
    val professors: List<String>,
    val room: String?,
    val slots: List<EnrollmentSlot>,
)

enum class EnrollmentShift { Morning, Afternoon, Night, Undefined }

data class EnrollmentSlot(
    val day: Int,
    val start: String,
    val end: String,
)

// The complete desired set submitted as one transaction.
data class EnrollmentSelection(
    val sectionId: Long,
    val allowsOther: Boolean,
    val waitlist: Boolean,
)
