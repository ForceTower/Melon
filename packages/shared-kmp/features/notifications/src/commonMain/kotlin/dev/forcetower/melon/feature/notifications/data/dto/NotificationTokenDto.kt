package dev.forcetower.melon.feature.notifications.data.dto

import kotlinx.serialization.Serializable

// `token` carries whichever push identifier is active — an FCM registration
// token or a Firebase Installation ID — and `identifierType` ("fcm_token" |
// "fid") tells the backend which one it is.
@Serializable
internal data class RegisterNotificationTokenRequest(
    val token: String,
    val identifierType: String,
    val platform: String,
    val deviceName: String? = null,
    val appVersion: String? = null,
    val locale: String? = null,
)

@Serializable
internal data class UnregisterNotificationTokenRequest(
    val token: String,
)
