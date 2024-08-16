package com.forcetower.uefs.core.model.edge.paradox

import com.google.gson.annotations.SerializedName

data class PublicTeacherSnapshot(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String
)