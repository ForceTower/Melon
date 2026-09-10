package dev.forcetower.melon.core.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "LectureMaterial",
    foreignKeys = [
        ForeignKey(
            entity = ClassLectureEntity::class,
            parentColumns = ["id"],
            childColumns = ["lectureId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("lectureId"), Index(value = ["lectureId", "platformId"], unique = true)],
)
data class LectureMaterialEntity(
    @PrimaryKey val id: String,
    val lectureId: String,
    // Long — upstream SAGRES material ids overflow Int (e.g. 8_000_011_956).
    // Backend column is `bigint`; mirrored here as Long to match.
    val platformId: Long,
    val description: String?,
    val url: String,
    val position: Int,
)
