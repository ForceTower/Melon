package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ClassLecture",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("classId"), Index(value = ["classId", "ordinal"], unique = true)],
)
data class ClassLectureEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val ordinal: Int,
    val situation: Int,
    val date: String?,
    val subject: String?,
)
