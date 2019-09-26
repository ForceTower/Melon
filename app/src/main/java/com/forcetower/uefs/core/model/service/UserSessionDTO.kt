package com.forcetower.uefs.core.model.service

import com.forcetower.uefs.core.model.unes.UserSession

data class UserSessionDTO(
    val start: Long,
    val end: Long,
    val sessions: List<UserSession>
)