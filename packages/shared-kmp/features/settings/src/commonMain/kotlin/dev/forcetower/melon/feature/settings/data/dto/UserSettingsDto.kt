package dev.forcetower.melon.feature.settings.data.dto

import kotlinx.serialization.Serializable

// Wire shape for the per-user settings row. Field names match the camelCase
// keys the API emits in `/api/sync/profile.settings` and accepts on
// `PATCH /api/me/settings`.
@Serializable
data class UserSettingsDto(
    val gradeSpoiler: Int,
    val notifMsgBroadcast: Boolean,
    val notifMsgClass: Boolean,
    val notifMsgDirect: Boolean,
    val notifGradePosted: Boolean,
    val notifGradeChanged: Boolean,
    val notifGradeDateChanged: Boolean,
    val notifClassLocation: Boolean,
    val notifClassMaterial: Boolean,
    val notifClassSubject: Boolean,
)

// Optional fields so the client only ships what changed; the API mirrors
// PATCH semantics on its side.
@Serializable
internal data class UpdateUserSettingsRequest(
    val gradeSpoiler: Int? = null,
    val notifMsgBroadcast: Boolean? = null,
    val notifMsgClass: Boolean? = null,
    val notifMsgDirect: Boolean? = null,
    val notifGradePosted: Boolean? = null,
    val notifGradeChanged: Boolean? = null,
    val notifGradeDateChanged: Boolean? = null,
    val notifClassLocation: Boolean? = null,
    val notifClassMaterial: Boolean? = null,
    val notifClassSubject: Boolean? = null,
)

@Serializable
internal data class UpdateUserSettingsResponse(
    val settings: UserSettingsDto,
)
