package com.forcetower.uefs.core.model.edge.account

import com.google.gson.annotations.SerializedName

data class ServiceStudentDTO(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("courseId")
    val courseId: String?,
    @SerializedName("courseName")
    val courseName: String?
)
