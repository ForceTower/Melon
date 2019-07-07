package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity
data class Flowchart(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    @SerializedName("course_id")
    val courseId: Long,
    val description: String,
    val lastUpdated: Long
)