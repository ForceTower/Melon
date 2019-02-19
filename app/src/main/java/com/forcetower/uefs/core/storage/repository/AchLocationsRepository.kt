/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

    fun onReceiveLocation(location: Location): List<AchDistance> {
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

    private fun matchesBigTray(location: Location): AchDistance {
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

    private fun matchesLibrary(location: Location): AchDistance {
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

    private fun matchesZoologyMuseum(location: Location): AchDistance {
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

    private fun matchesHogwarts(location: Location): AchDistance {
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

    private fun matchesMod1(location: Location): AchDistance {
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

    private fun matchesMod7(location: Location): AchDistance {
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

    private fun matchesManagement(location: Location): AchDistance {
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
