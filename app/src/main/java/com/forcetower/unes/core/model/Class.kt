package com.forcetower.unes.core.model

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import java.util.*

@Entity(
    foreignKeys = [
        ForeignKey(entity = Discipline::class, parentColumns = ["uid"], childColumns = ["discipline_id"], onDelete = CASCADE, onUpdate = CASCADE),
        ForeignKey(entity = Semester::class, parentColumns = ["uid"], childColumns = ["semester_id"], onDelete = CASCADE, onUpdate = CASCADE)
    ], indices = [
        Index(value = ["discipline_id", "semester_id", "code"], unique = true),
        Index(value = ["uuid"], unique = true)
    ]
)
data class Class(
    @PrimaryKey(autoGenerate = true)
    val uid: Long,
    @ColumnInfo(name = "discipline_id")
    val disciplineId: Long,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
    val code: String,
    val teacher: String? = null,
    val status: String? = null,
    val uuid: String = UUID.randomUUID().toString(),
    val credits: Int? = null
)