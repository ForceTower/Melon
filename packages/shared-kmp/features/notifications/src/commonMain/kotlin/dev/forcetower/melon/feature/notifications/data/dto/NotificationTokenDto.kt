package dev.forcetower.melon.feature.notifications.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class RegisterNotificationTokenRequest(
    val token: String,
    val platform: String,
    val deviceName: String? = null,
    val appVersion: String? = null,
    val locale: String? = null,
)
