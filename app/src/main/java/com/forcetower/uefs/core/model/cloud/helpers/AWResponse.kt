package com.forcetower.uefs.core.model.cloud.helpers

data class AWResponse<T> (
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null
)