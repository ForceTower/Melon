package com.forcetower.uefs.core.model.edge

import com.google.gson.annotations.SerializedName

data class EdgeAccessToken(
    @SerializedName("accessToken")
    val accessToken: String
)