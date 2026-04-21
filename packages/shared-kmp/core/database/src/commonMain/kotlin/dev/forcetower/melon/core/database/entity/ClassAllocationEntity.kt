package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ClassAllocation",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ClassSpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("classId"), Index("spaceId")],
)
data class ClassAllocationEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val spaceId: String?,
    val timePlatformId: Long?,
    // Day-of-week encoded as 0=Sunday..6=Saturday (Snowpiercer `time.day`,
    // stored untransformed by apps/api sync). NOT ISO weekday and NOT the
    // Java Calendar.DAY_OF_WEEK convention — consumers targeting a
    // Monday..Sunday layout must shift, e.g. `(day + 6) % 7`.
    val day: Int?,
    val startTime: String?,
    val endTime: String?,
)
