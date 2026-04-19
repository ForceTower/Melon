package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "StudentClass",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("classId")],
)
data class StudentClassEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val finalGrade: String?,
    val missedClasses: Int?,
    val resultDescription: String?,
    val approved: Boolean?,
    val underRevision: Boolean,
    val wentToFinals: Boolean,
    val resultSyncedAt: String?,
)
