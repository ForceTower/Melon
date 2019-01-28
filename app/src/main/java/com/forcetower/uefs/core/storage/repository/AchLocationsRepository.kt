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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchLocationsRepository @Inject constructor(
    private val preferences: SharedPreferences
) {

    fun onReceiveLocation(location: Location): Int? {
        return matchesBigTray(location)
                ?: matchesLibrary(location)
                ?: matchesZoologyMuseum(location)
                ?: matchesHogwarts(location)
                ?: matchesMod1(location)
                ?: matchesMod7(location)
                ?: matchesManagement(location)
    }

    private fun matchesBigTray(location: Location): Int? {
        val place = Location("").apply {
            latitude = -12.201868
            longitude = -38.96974
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_big_tray_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_big_tray", true).apply()
            return R.string.achievement_bandejo
        }
        return null
    }

    private fun matchesLibrary(location: Location): Int? {
        val place = Location("").apply {
            latitude = -12.202193
            longitude = -38.972065
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_library_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_library", true).apply()
            return R.string.achievement_dora_a_estudiosa
        }
        return null
    }

    private fun matchesZoologyMuseum(location: Location): Int? {
        val place = Location("").apply {
            latitude = -12.198888
            longitude = -38.967986
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_zoology_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_zoology", true).apply()
            return R.string.achievement_dora_a_aventureira
        }
        return null
    }

    private fun matchesHogwarts(location: Location): Int? {
        val place = Location("").apply {
            latitude = -12.198144
            longitude = -38.971951
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_hogwarts_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_hogwarts", true).apply()
            return R.string.achievement_dora_a_misteriosa
        }
        return null
    }

    private fun matchesMod1(location: Location): Int? {
        val place = Location("").apply {
            latitude = -12.199827
            longitude = -38.969190
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_mod1_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_mod1", true).apply()
            return R.string.achievement_dora_temporada_1
        }
        return null
    }

    private fun matchesMod7(location: Location): Int? {
        val place = Location("").apply {
            latitude = -12.201418
            longitude = -38.975059
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_mod7_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_mod7", true).apply()
            return R.string.achievement_dora_temporada_7
        }
        return null
    }

    private fun matchesManagement(location: Location): Int? {
        val place = Location("").apply {
            latitude = -12.202269
            longitude = -38.971030
        }
        val distance = location.distanceTo(place)
        preferences.edit().putFloat("ach_dora_management_dist", distance).apply()
        if (distance - location.accuracy <= 30) {
            preferences.edit().putBoolean("ach_dora_management", true).apply()
            return R.string.achievement_dora_descansando
        }
        return null
    }
}
