package com.forcetower.uefs.core.model.edge

import com.google.gson.annotations.SerializedName

data class EdgeAccessTokenDTO(
    @SerializedName("accessToken")
    val accessToken: String
)