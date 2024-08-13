package com.forcetower.uefs.core.model.edge

import com.google.gson.annotations.SerializedName

data class LinkEmailResponseDTO(
    @SerializedName("securityToken")
    val securityToken: String
)
