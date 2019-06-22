package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(indices = [
    Index(value = ["name"])
])
data class STeacher(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("teacher_id")
    val teacherId: Long,
    val name: String,
    @SerializedName("image_url")
    val imageUrl: String?
)