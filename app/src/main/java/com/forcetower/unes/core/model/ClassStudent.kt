package com.forcetower.unes.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ClassStudent(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val profileId: Long,
    val classId: Long,
    val finalGrade: Double?
)