package dev.forcetower.melon.feature.auth.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
internal data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: LoginUserDto,
)

@Serializable
internal data class LoginUserDto(
    val id: String,
    val name: String,
    val imageUrl: String?,
)
