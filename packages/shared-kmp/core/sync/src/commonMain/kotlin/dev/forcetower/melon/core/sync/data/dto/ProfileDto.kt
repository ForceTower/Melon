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
@Serializable
internal data class ProfileSettingsDto(
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
