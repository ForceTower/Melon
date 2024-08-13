package com.forcetower.uefs.core.model.edge

import com.google.gson.annotations.SerializedName

data class ServiceResponseWrapper<T>(
    @SerializedName("ok")
    val ok: Boolean,
    @SerializedName("data")
    val data: T,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("error")
    val error: String? = null
)
