package com.forcetower.uefs.core.model.edge

import com.google.gson.annotations.SerializedName

data class EmailLinkBodyDTO(
    @SerializedName("email")
    val email: String
)
