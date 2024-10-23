package com.forcetower.uefs.core.model.edge.account

import com.google.gson.annotations.SerializedName

data class ServiceAccountDTO(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null
)
