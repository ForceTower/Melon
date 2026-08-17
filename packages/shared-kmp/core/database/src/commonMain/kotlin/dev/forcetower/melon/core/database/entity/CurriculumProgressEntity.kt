package dev.forcetower.melon.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// The student's headline hour counts plus the payload-level facts that don't
// belong to any single entry. Single row keyed by [CURRENT]; it exists
// independently of [CurriculumEntity] because the portal can answer with hours
// for a course whose grid we don't hold — `curriculumId` is null then, and the
// screen renders hours with no denominator.
@Entity(tableName = "CurriculumProgress")
data class CurriculumProgressEntity(
    @PrimaryKey val key: String = CURRENT,
    val curriculumId: String?,
    val completedHours: Int,
    // Null when no curriculum is held for the course.
    val requiredHours: Int?,
    // Capped per requirement so surplus electives can't push it past 100.
    val percent: Double?,
    // Of `requiredHours`, how much sits in buckets whose completion is never
    // observable (paper certificates).
    val excludedHours: Int,
    // Completed hours whose requirement bucket is unknown — counted in the
    // total, absent from the per-nature bars.
    val unclassifiedHours: Int,
    val disciplinesCompleted: Int,
    val disciplinesTotal: Int,
    // Where the student is now — the highest período they have work in.
    val currentPeriod: Int?,
    // False when too few entries carry prerequisites to claim availability.
    val prerequisitesKnown: Boolean,
    // Epoch millis of the refresh that wrote this row.
    val syncedAt: Long,
    // Every hour the student has passed, whichever version lists it — what
    // each version's `fit` is a share of.
    @ColumnInfo(defaultValue = "0") val approvedHours: Int = 0,
) {
    companion object {
        const val CURRENT = "current"
    }
}
