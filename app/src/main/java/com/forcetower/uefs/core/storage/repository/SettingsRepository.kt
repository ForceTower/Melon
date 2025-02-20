/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.core.storage.repository

import com.forcetower.uefs.core.storage.database.UDatabase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SettingsRepository @Inject constructor(
    private val database: UDatabase,
    private val gradesRepository: SagresGradesRepository,
    private val adventureRepository: AdventureRepository
) {

    suspend fun requestAllGradesAndCalculateScore() = withContext(Dispatchers.IO) {
        var loginNeeded = true
        val semesters = database.semesterDao().getSemestersDirect()
        semesters.forEach {
            val result = gradesRepository.getGrades(it.sagresId, loginNeeded)
            loginNeeded = false
            if (result != 0) {
                Timber.d("Failed to run on semester ${it.sagresId} - ${it.codename}: $result")
            }
        }
        adventureRepository.performCheckAchievements(HashMap())
    }
}
