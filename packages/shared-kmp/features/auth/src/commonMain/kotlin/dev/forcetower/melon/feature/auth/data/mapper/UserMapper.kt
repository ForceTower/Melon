package dev.forcetower.melon.feature.auth.data.mapper

import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.feature.auth.data.dto.LoginUserDto

internal fun LoginUserDto.toDomain(): User {
    return User(id = id, name = name, imageUrl = imageUrl)
}
