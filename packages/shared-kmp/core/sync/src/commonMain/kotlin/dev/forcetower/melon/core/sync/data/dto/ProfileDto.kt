package dev.forcetower.melon.core.sync.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileResponse(
    val user: UserDto,
    val student: StudentDto,
    val course: CourseDto?,
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
