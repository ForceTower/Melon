package com.forcetower.uefs.core.model.edge.paradox

import com.google.gson.annotations.SerializedName

data class PublicDisciplineSnapshot(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("department")
    val department: String
)