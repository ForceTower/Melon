package dev.forcetower.melon.feature.enrollment.data.dto

import kotlinx.serialization.Serializable

// Wire shapes for the screen-ready matrícula endpoints. Field names match the
// camelCase keys the API emits; the backend already flattened the SAGRES offer
// tree and enum-mapped the shift, so these decode 1:1.

@Serializable
internal data class EnrollmentWindowResponse(
    val available: Boolean = false,
    val window: EnrollmentWindowDto? = null,
)

@Serializable
internal data class EnrollmentWindowDto(
    val semester: String,
    val state: String, // OPEN | UPCOMING | CLOSED
    val startDate: String,
    val endDate: String,
    val minHours: Int,
    val maxHours: Int,
    val useQueue: Boolean,
    val courseId: Long,
)

@Serializable
internal data class EnrollmentOffersResponse(
    val disciplines: List<EnrollmentDisciplineDto> = emptyList(),
)

@Serializable
internal data class EnrollmentDisciplineDto(
    val id: Long,
    val code: String,
    val name: String,
    val workload: Int,
    val mandatory: Boolean,
    val gradePeriod: Int,
    val suggestion: Boolean = false,
    val prereqs: List<EnrollmentPrereqDto> = emptyList(),
    val sections: List<EnrollmentSectionDto> = emptyList(),
)

@Serializable
internal data class EnrollmentPrereqDto(
    val code: String,
    val name: String,
    val met: Boolean = true,
)

@Serializable
internal data class EnrollmentSectionDto(
    val id: Long,
    val label: String,
    val coursePreferential: Boolean = false,
    val suggestion: Boolean = false,
    val vacancies: Int,
    val proposalsCount: Int,
    val allowsOtherDefault: Boolean = false,
    val waitlistCount: Int = 0,
    val selected: Boolean = false,
    val meetings: List<EnrollmentMeetingDto> = emptyList(),
)

@Serializable
internal data class EnrollmentMeetingDto(
    val kind: String,
    val shift: String, // MORNING | AFTERNOON | NIGHT | UNDEFINED
    val professors: List<String> = emptyList(),
    val room: String? = null,
    val slots: List<EnrollmentSlotDto> = emptyList(),
)

@Serializable
internal data class EnrollmentSlotDto(
    val day: Int,
    val start: String,
    val end: String,
)

@Serializable
internal data class SubmitEnrollmentRequest(
    val selections: List<EnrollmentSelectionDto>,
)

@Serializable
internal data class EnrollmentSelectionDto(
    val sectionId: Long,
    val allowsOther: Boolean,
    val waitlist: Boolean,
)
