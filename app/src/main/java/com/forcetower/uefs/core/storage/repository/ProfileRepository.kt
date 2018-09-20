package com.forcetower.uefs.core.storage.repository

import com.forcetower.uefs.core.storage.database.UDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    val database: UDatabase
) {
    fun getMeProfile() = database.profileDao().selectMe()
}