package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val platformId: Int,
    val description: String?,
    val url: String,
    val position: Int,
)
