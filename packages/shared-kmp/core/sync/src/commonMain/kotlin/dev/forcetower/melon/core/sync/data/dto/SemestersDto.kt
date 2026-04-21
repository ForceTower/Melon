package dev.forcetower.melon.core.sync.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class SemesterListResponse(
    val semesters: List<SemesterListItemDto>,
)

@Serializable
internal data class SemesterListItemDto(
    val id: String,
    val platformId: Long,
    val code: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val track: String?,
    val dirtyAt: String?,
)

@Serializable
internal data class SemesterPayloadResponse(
    val semester: SemesterDto,
    val disciplines: List<DisciplineDto>,
    val disciplineOffers: List<DisciplineOfferDto>,
    val classes: List<ClassDto>,
    val teachers: List<TeacherDto>,
    val classTeachers: List<ClassTeacherDto>,
    val spaces: List<SpaceDto>,
    val allocations: List<AllocationDto>,
    val studentClasses: List<StudentClassDto>,
    val evaluations: List<EvaluationDto>,
    val studentGrades: List<StudentGradeDto>,
    val lectures: List<LectureDto>,
    val lectureMaterials: List<LectureMaterialDto>,
)

@Serializable
internal data class SemesterDto(
    val id: String,
    val platformId: Long,
    val code: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val track: String?,
)

@Serializable
internal data class DisciplineDto(
    val id: String,
    val code: String,
    val platformId: Long?,
    val name: String,
    val hours: Int,
    val department: String?,
    val program: String?,
)

@Serializable
internal data class DisciplineOfferDto(
    val id: String,
    val disciplineId: String,
    val semesterId: String,
    val platformId: Long,
    val hours: Int?,
    val program: String?,
)

@Serializable
internal data class ClassDto(
    val id: String,
    val offerId: String,
    val platformId: Long,
    val groupName: String,
    val type: String,
    val hours: Int,
    val program: String?,
)

@Serializable
internal data class TeacherDto(
    val id: String,
    val platformId: Long,
    val name: String,
)

@Serializable
internal data class ClassTeacherDto(
    val classId: String,
    val teacherId: String,
)

@Serializable
internal data class SpaceDto(
    val id: String,
    val platformId: Long,
    val type: String?,
    val campus: String,
    val location: String,
    val modulo: String,
)

@Serializable
internal data class AllocationDto(
    val id: String,
    val classId: String,
    val spaceId: String?,
    val timePlatformId: Long?,
    val day: Int?,
    val startTime: String?,
    val endTime: String?,
)

@Serializable
internal data class StudentClassDto(
    val id: String,
    val classId: String,
    val finalGrade: String?,
    val missedClasses: Int?,
    val resultDescription: String?,
    val approved: Boolean?,
    val underRevision: Boolean,
    val wentToFinals: Boolean,
    val resultSyncedAt: String?,
)

@Serializable
internal data class EvaluationDto(
    val id: String,
    val classId: String,
    val platformId: String,
    val name: String?,
    val position: Int,
)

@Serializable
internal data class StudentGradeDto(
    val id: String,
    val studentClassId: String,
    val evaluationId: String,
    val platformId: String,
    val name: String,
    val nameShort: String?,
    val ordinal: Int,
    val weight: String,
    val value: String?,
    val date: String?,
)

@Serializable
internal data class LectureDto(
    val id: String,
    val classId: String,
    val ordinal: Int,
    val situation: Int,
    val date: String?,
    val subject: String?,
)

@Serializable
internal data class LectureMaterialDto(
    val id: String,
    val lectureId: String,
    // Long — upstream SAGRES material ids overflow Int (e.g. 8_000_011_956).
    // Backend column is `bigint`; mirrored here as Long to match.
    val platformId: Long,
    val description: String?,
    val url: String,
    val position: Int,
)
