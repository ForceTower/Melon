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
package com.forcetower.uefs.feature.themeswitcher

import android.content.Context
import android.content.SharedPreferences
import android.util.SparseIntArray
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatDelegate
import com.forcetower.uefs.R

class ThemePreferencesManager(private val context: Context, private val resourceProvider: ThemeSwitcherResourceProvider) {

    fun saveAndApplyTheme(@IdRes id: Int) {
        val nightMode = convertToNightMode(id)
        saveNightMode(nightMode)
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    @get:IdRes
    val currentThemeId: Int
        get() = convertToThemeId(nightMode)

    val themeIds: IntArray
        get() {
            val themeIds = IntArray(THEME_NIGHT_MODE_MAP.size())
            for (i in 0 until THEME_NIGHT_MODE_MAP.size()) {
                themeIds[i] = THEME_NIGHT_MODE_MAP.keyAt(i)
            }
            return themeIds
        }

    private val nightMode: Int
        get() = sharedPreferences.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    private fun saveNightMode(nightMode: Int) {
        sharedPreferences.edit().putInt(KEY_NIGHT_MODE, nightMode).apply()
    }

    private val sharedPreferences: SharedPreferences
        get() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun convertToNightMode(@IdRes id: Int): Int {
        return THEME_NIGHT_MODE_MAP[id, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM]
    }

    @IdRes
    private fun convertToThemeId(nightMode: Int): Int {
        return THEME_NIGHT_MODE_MAP.keyAt(THEME_NIGHT_MODE_MAP.indexOfValue(nightMode))
    }

    companion object {
        private const val PREFERENCES_NAME = "night_mode_preferences"
        private const val KEY_NIGHT_MODE = "night_mode"
        private val THEME_NIGHT_MODE_MAP = SparseIntArray()

        init {
            THEME_NIGHT_MODE_MAP.append(R.id.theme_light, AppCompatDelegate.MODE_NIGHT_NO)
            THEME_NIGHT_MODE_MAP.append(R.id.theme_dark, AppCompatDelegate.MODE_NIGHT_YES)
            THEME_NIGHT_MODE_MAP.append(R.id.theme_default, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}