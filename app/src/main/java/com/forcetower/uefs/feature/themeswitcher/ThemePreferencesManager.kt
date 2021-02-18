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

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.SparseIntArray
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import com.forcetower.uefs.R
import timber.log.Timber

class ThemePreferencesManager(private val context: Context) {
    private val provider = ThemeSwitcherResourceProvider()

    fun saveAndApplyTheme(@IdRes id: Int) {
        val nightMode = convertToNightMode(id)
        saveNightMode(nightMode)
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun deleteSavedTheme() {
        saveColors(0, 0, 0)
    }

    fun retrieveOverlay() {
        setThemeOverlayWithoutActivity(primaryColor, secondaryColor, backgroundColor)
    }

    @get:IdRes
    val currentThemeId: Int
        get() = convertToThemeId(nightMode)

    private val nightMode: Int
        get() = sharedPreferences.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    private val primaryColor: Int
        get() = resourceFromIndex(provider.primaryColors, sharedPreferences.getInt(KEY_PRIMARY_COLOR, -1))

    private val secondaryColor: Int
        get() = resourceFromIndex(provider.secondaryColors, sharedPreferences.getInt(KEY_SECONDARY_COLOR, -1))

    private val backgroundColor: Int
        get() = resourceFromIndex(provider.backgroundColors, sharedPreferences.getInt(KEY_BACKGROUND_COLOR, -1))

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

    fun clearThemeOverlays(activity: Activity) {
        saveColors(0, 0, 0)
        ThemeOverlayUtils.clearThemeOverlays(activity)
    }

    fun saveColorsAndApplyThemeOverlay(activity: Activity, primary: Int, secondary: Int, background: Int) {
        saveColors(primary, secondary, background)
        applyThemeOverlay(activity, primary, secondary, background)
    }

    private fun setThemeOverlayWithoutActivity(primary: Int, secondary: Int, background: Int) {
        val themesMap = arrayOf(
            intArrayOf(R.id.theme_feature_primary_color, primary),
            intArrayOf(R.id.theme_feature_secondary_color, secondary),
            intArrayOf(R.id.theme_feature_background_color, background)
        )
        for (i in themesMap.indices) {
            if (themesMap[i][1] != 0)
                ThemeOverlayUtils.setThemeOverlay(themesMap[i][0], themesMap[i][1])
        }
    }

    private fun applyThemeOverlay(activity: Activity, primary: Int, secondary: Int, background: Int) {
        val themesMap = arrayOf(
            intArrayOf(R.id.theme_feature_primary_color, primary),
            intArrayOf(R.id.theme_feature_secondary_color, secondary),
            intArrayOf(R.id.theme_feature_background_color, background)
        )
        for (i in themesMap.indices) {
            if (themesMap[i][1] != 0)
                ThemeOverlayUtils.setThemeOverlay(themesMap[i][0], themesMap[i][1])
        }

        activity.recreate()
    }

    private fun saveColors(primary: Int, secondary: Int, background: Int) {
        val positional = convertThemeToPositionalData(ThemeStyleData(primary, secondary, background))
        sharedPreferences.edit()
            .putInt(KEY_PRIMARY_COLOR, positional.primary)
            .putInt(KEY_SECONDARY_COLOR, positional.secondary)
            .putInt(KEY_BACKGROUND_COLOR, positional.background)
            .apply()
    }

    private fun convertThemeToPositionalData(data: ThemeStyleData): ThemePositionData {
        val primaryPosition = indexOfResource(provider.primaryColors, data.primary)
        val secondaryPosition = indexOfResource(provider.secondaryColors, data.secondary)
        val backgroundPosition = indexOfResource(provider.backgroundColors, data.background)
        return ThemePositionData(primaryPosition, secondaryPosition, backgroundPosition)
    }

    private fun convertPositionToThemeData(data: ThemePositionData): ThemeStyleData {
        val primary = resourceFromIndex(provider.primaryColors, data.primary)
        val secondary = resourceFromIndex(provider.secondaryColors, data.secondary)
        val background = resourceFromIndex(provider.backgroundColors, data.background)
        return ThemeStyleData(primary, secondary, background)
    }

    private fun indexOfResource(arrayId: Int, elementId: Int): Int {
        if (elementId == 0) return -1
        val resources = context.resources
        val typed = resources.obtainTypedArray(arrayId)
        var pos = 0
        var found = false
        while (pos < typed.length()) {
            if (typed.getResourceId(pos, 0) == elementId) {
                found = true
                break
            }
            pos++
        }
        typed.recycle()
        return if (found) pos else -1
    }

    private fun resourceFromIndex(arrayId: Int, position: Int): Int {
        if (position == -1) return 0
        val resources = context.resources
        val typed = resources.obtainTypedArray(arrayId)
        return try {
            typed.getResourceId(position, 0)
        } catch (error: Throwable) {
            Timber.d(error, "Error retrieving theme...")
            0
        } finally {
            typed.recycle()
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "theme_mode_preferences"
        private const val KEY_NIGHT_MODE = "night_mode"
        private const val KEY_PRIMARY_COLOR = "_themed_primary_color"
        private const val KEY_SECONDARY_COLOR = "_themed_secondary_color"
        private const val KEY_BACKGROUND_COLOR = "_themed_background_color"
        private val THEME_NIGHT_MODE_MAP = SparseIntArray()

        init {
            THEME_NIGHT_MODE_MAP.append(R.id.theme_light, AppCompatDelegate.MODE_NIGHT_NO)
            THEME_NIGHT_MODE_MAP.append(R.id.theme_dark, AppCompatDelegate.MODE_NIGHT_YES)
            THEME_NIGHT_MODE_MAP.append(R.id.theme_default, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private data class ThemeStyleData(
        @StyleRes val primary: Int,
        @StyleRes val secondary: Int,
        @StyleRes val background: Int
    )

    private data class ThemePositionData(
        val primary: Int,
        val secondary: Int,
        val background: Int
    )
}
