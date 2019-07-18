package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(indices = [
    Index(value = ["name"])
])
data class SStudent(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("student_id")
    val id: Long,
    val name: String,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("course_id")
    val course: Int?,
    @SerializedName("course_name")
    val courseName: String?,
    val me: Boolean = false
)

data class SStudentDTO(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("student_id")
    val id: Long,
    val name: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("course_id")
    val course: Int?,
    @SerializedName("course_name")
    val courseName: String?,
    val me: Boolean?,
    val statements: List<ProfileStatement>? = null
) {
    fun toCommon() = SStudent(id, name ?: "Desconhecido", imageUrl, course, courseName, me ?: false)
}
