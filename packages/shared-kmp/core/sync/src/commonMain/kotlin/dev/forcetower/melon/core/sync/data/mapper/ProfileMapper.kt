package dev.forcetower.melon.core.sync.data.mapper

import dev.forcetower.melon.core.database.entity.CourseEntity
import dev.forcetower.melon.core.database.entity.StudentEntity
import dev.forcetower.melon.core.database.entity.UserEntity
import dev.forcetower.melon.core.sync.data.dto.CourseDto
import dev.forcetower.melon.core.sync.data.dto.StudentDto
import dev.forcetower.melon.core.sync.data.dto.UserDto

internal fun UserDto.toEntity(): UserEntity =
    UserEntity(id = id, name = name, imageUrl = imageUrl, email = email)

internal fun StudentDto.toEntity(): StudentEntity =
    StudentEntity(id = id, platformId = platformId, name = name, courseId = courseId)

internal fun CourseDto.toEntity(): CourseEntity =
    CourseEntity(id = id, platformId = platformId, name = name, resumedName = resumedName)
