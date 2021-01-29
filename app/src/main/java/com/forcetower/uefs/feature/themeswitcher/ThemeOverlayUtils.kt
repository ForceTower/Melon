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
import android.util.SparseIntArray
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.core.util.forEach
import androidx.core.util.set

object ThemeOverlayUtils {
    @JvmStatic
    private val themeOverlays = SparseIntArray()

    @JvmStatic
    fun setThemeOverlay(@IdRes id: Int, @StyleRes themeOverlay: Int) {
        themeOverlays[id] = themeOverlay
    }

    @JvmStatic
    fun clearThemeOverlays(activity: Activity) {
        themeOverlays.clear()
        activity.recreate()
    }

    @JvmStatic
    fun clearThemeOverlays() {
        themeOverlays.clear()
    }

    @JvmStatic
    fun getThemeOverlay(@IdRes id: Int): Int {
        return themeOverlays[id]
    }

    @JvmStatic
    fun applyThemeOverlays(activity: Activity, except: IntArray = IntArray(0)) {
        themeOverlays.forEach { key, value ->
            if (!except.contains(key)) activity.setTheme(value)
        }
    }
}
