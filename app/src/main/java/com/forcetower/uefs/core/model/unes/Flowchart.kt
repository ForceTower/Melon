package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Flowchart(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val courseId: Long,
    val description: String,
    val lastUpdated: Long
)