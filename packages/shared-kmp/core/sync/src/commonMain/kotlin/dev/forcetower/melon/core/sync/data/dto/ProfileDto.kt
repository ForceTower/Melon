package dev.forcetower.melon.core.sync.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileResponse(
    val user: UserDto,
    val student: StudentDto,
    val course: CourseDto?,
    val lastSyncCompletedAt: String? = null,
    val settings: ProfileSettingsDto,
)

// Same wire shape as `feature/settings/data/dto/UserSettingsDto`, kept
// duplicated here so `core/sync` doesn't have to depend on the settings
// feature module (and risk a cyclic graph). The mapper in
// `ProfileMapper.toEntity` is the only place that needs this shape.
//
// Defaults mirror migration `016_user_settings_preferences` so the client
// stays usable against an older API that hasn't shipped those columns yet.
@Serializable
internal data class ProfileSettingsDto(
    val gradeSpoiler: Int = 1,
    val notifMsgBroadcast: Boolean = true,
    val notifMsgClass: Boolean = true,
    val notifMsgDirect: Boolean = true,
    val notifGradePosted: Boolean = true,
    val notifGradeChanged: Boolean = true,
    val notifGradeDateChanged: Boolean = false,
    val notifClassLocation: Boolean = true,
    val notifClassMaterial: Boolean = true,
    val notifClassSubject: Boolean = false,
)

@Serializable
internal data class UserDto(
    val id: String,
    val name: String,
    val email: String?,
    val imageUrl: String?,
)

@Serializable
internal data class StudentDto(
    val id: String,
    val platformId: Long,
    val name: String,
    val courseId: String?,
)

@Serializable
internal data class CourseDto(
    val id: String,
    val platformId: Long,
    val name: String,
    val resumedName: String,
)
