package dev.forcetower.melon.feature.auth.data

import dev.forcetower.melon.core.session.User

internal fun LoginUserDto.toDomain(): User =
    User(id = id, name = name, imageUrl = imageUrl)
