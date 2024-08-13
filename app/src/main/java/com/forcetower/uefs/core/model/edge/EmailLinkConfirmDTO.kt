package com.forcetower.uefs.core.model.edge

import com.google.gson.annotations.SerializedName

data class EmailLinkConfirmDTO(
    @SerializedName("code")
    val code: String,
    @SerializedName("securityToken")
    val securityToken: String
)
