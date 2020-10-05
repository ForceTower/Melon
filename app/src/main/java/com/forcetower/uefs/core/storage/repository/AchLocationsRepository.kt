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

import android.content.SharedPreferences
import android.location.Location
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.AchDistance
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchLocationsRepository @Inject constructor(
    private val preferences: SharedPreferences
) {

    fun onReceiveLocation(location: Location?): List<AchDistance> {
        val result = mutableListOf<AchDistance>()
        result += matchesBigTray(location)
        result += matchesLibrary(location)
        result += matchesZoologyMuseum(location)
        result += matchesHogwarts(location)
        result += matchesMod1(location)
        result += matchesMod7(location)
        result += matchesManagement(location)
        return result
    }

    private fun matchesBigTray(location: Location?): AchDistance {
        location ?: return AchDistance(null, "Bandejao", -1.0f)
        val place = Location("").apply {
            latitude = -12.201868
            longitude = -38.96974
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_big_tray_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_big_tray", true).apply()
            return AchDistance(R.string.achievement_bandejo, "Bandejão", distance)
        }
        return AchDistance(null, "Bandejao", distance)
    }

    private fun matchesLibrary(location: Location?): AchDistance {
        location ?: return AchDistance(null, "Biblioteca", -1.0f)
        val place = Location("").apply {
            latitude = -12.202193
            longitude = -38.972065
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_library_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_library", true).apply()
            return AchDistance(R.string.achievement_dora_a_estudiosa, "Biblioteca", distance)
        }
        return AchDistance(null, "Biblioteca", distance)
    }

    private fun matchesZoologyMuseum(location: Location?): AchDistance {
        location ?: return AchDistance(null, "Serpentário", -1.0f)
        val place = Location("").apply {
            latitude = -12.196325
            longitude = -38.970038
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_zoology_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_zoology", true).apply()
            return AchDistance(R.string.achievement_dora_a_aventureira, "Serpentário", distance)
        }
        return AchDistance(null, "Serpentário", distance)
    }

    private fun matchesHogwarts(location: Location?): AchDistance {
        location ?: return AchDistance(null, "Hogwarts", -1.0f)
        val place = Location("").apply {
            latitude = -12.198144
            longitude = -38.971951
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_hogwarts_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_hogwarts", true).apply()
            return AchDistance(R.string.achievement_dora_a_misteriosa, "Hogwarts", distance)
        }
        return AchDistance(null, "Hogwarts", distance)
    }

    private fun matchesMod1(location: Location?): AchDistance {
        location ?: return AchDistance(null, "Módulo 1", -1.0f)
        val place = Location("").apply {
            latitude = -12.199827
            longitude = -38.969190
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_mod1_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_mod1", true).apply()
            return AchDistance(R.string.achievement_dora_temporada_1, "Módulo 1", distance)
        }
        return AchDistance(null, "Módulo 1", distance)
    }

    private fun matchesMod7(location: Location?): AchDistance {
        location ?: return AchDistance(null, "Módulo 7", -1.0f)
        val place = Location("").apply {
            latitude = -12.201418
            longitude = -38.975059
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_mod7_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_mod7", true).apply()
            return AchDistance(R.string.achievement_dora_temporada_7, "Módulo 7", distance)
        }
        return AchDistance(null, "Módulo 7", distance)
    }

    private fun matchesManagement(location: Location?): AchDistance {
        location ?: return AchDistance(null, "Reitoria", -1.0f)
        val place = Location("").apply {
            latitude = -12.202269
            longitude = -38.971030
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_management_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_management", true).apply()
            return AchDistance(R.string.achievement_dora_descansando, "Reitoria", distance)
        }
        return AchDistance(null, "Reitoria", distance)
    }
}
