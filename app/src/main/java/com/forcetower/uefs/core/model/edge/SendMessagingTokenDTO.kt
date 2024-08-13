package com.forcetower.uefs.core.model.edge

import com.google.gson.annotations.SerializedName

data class SendMessagingTokenDTO(
    @SerializedName("token")
    val token: String
)
