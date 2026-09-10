package dev.forcetower.melon.core.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
