package dev.forcetower.melon.core.sync.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class MyCredentialsResponse(
    val credentials: UpstreamCredentialsDto? = null,
)

@Serializable
internal data class UpstreamCredentialsDto(
    val username: String,
    val password: String,
)
