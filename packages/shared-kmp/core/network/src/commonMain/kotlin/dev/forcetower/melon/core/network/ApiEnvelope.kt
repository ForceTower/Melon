package dev.forcetower.melon.core.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val ok: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null,
)
