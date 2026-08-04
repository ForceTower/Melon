package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Semester")
data class SemesterEntity(
    @PrimaryKey val id: String,
    val platformId: Long,
    val code: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val track: String?,
    // Last time the worker applied this student's semester subtree
    // server-side. Only the semester list carries it.
    val dirtyAt: String? = null,
    // The `dirtyAt` in effect when this semester's payload was last
    // mirrored. Only the client writes it — a mirrored semester whose
    // server `dirtyAt` moved past this is stale and re-syncs on refresh.
    val appliedDirtyAt: String? = null,
)
