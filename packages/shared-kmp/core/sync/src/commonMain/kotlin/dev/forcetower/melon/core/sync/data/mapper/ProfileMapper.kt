package dev.forcetower.melon.core.sync.data.mapper

import dev.forcetower.melon.core.database.entity.CourseEntity
import dev.forcetower.melon.core.database.entity.StudentEntity
import dev.forcetower.melon.core.database.entity.UserEntity
import dev.forcetower.melon.core.database.entity.UserSettingsEntity
import dev.forcetower.melon.core.sync.data.dto.CourseDto
import dev.forcetower.melon.core.sync.data.dto.ProfileSettingsDto
import dev.forcetower.melon.core.sync.data.dto.StudentDto
import dev.forcetower.melon.core.sync.data.dto.UserDto

internal fun UserDto.toEntity(): UserEntity =
    UserEntity(id = id, name = name, imageUrl = imageUrl, email = email)

internal fun StudentDto.toEntity(lastSyncCompletedAt: String? = null): StudentEntity =
    StudentEntity(
        id = id,
        platformId = platformId,
        name = name,
        courseId = courseId,
        lastSyncCompletedAt = lastSyncCompletedAt,
    )

internal fun CourseDto.toEntity(): CourseEntity =
    CourseEntity(id = id, platformId = platformId, name = name, resumedName = resumedName)

internal fun ProfileSettingsDto.toEntity(userId: String): UserSettingsEntity =
    UserSettingsEntity(
        userId = userId,
        gradeSpoiler = gradeSpoiler,
        notifMsgBroadcast = notifMsgBroadcast,
        notifMsgClass = notifMsgClass,
        notifMsgDirect = notifMsgDirect,
        notifGradePosted = notifGradePosted,
        notifGradeChanged = notifGradeChanged,
        notifGradeDateChanged = notifGradeDateChanged,
        notifClassLocation = notifClassLocation,
        notifClassMaterial = notifClassMaterial,
        notifClassSubject = notifClassSubject,
    )
