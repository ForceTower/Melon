package dev.forcetower.melon.core.database.query

// Projection returned by AcademicDao.getSemesterAggregate. `totalHours` sums
// Class.hours across every class tied to the semester — displayed in the UI as
// "créditos".
data class SemesterClassAggregate(
    val classCount: Int,
    val totalHours: Int,
)

// One row per ClassAllocation, pre-joined with the enclosing class's
// discipline, primary teacher, and room. `day` follows the upstream encoding
// (stored as-is from the source platform).
data class SemesterAllocationRow(
    val allocationId: String,
    val classId: String,
    val disciplineName: String,
    val day: Int?,
    val startTime: String?,
    val endTime: String?,
    val spaceLocation: String?,
    val teacherName: String?,
)
