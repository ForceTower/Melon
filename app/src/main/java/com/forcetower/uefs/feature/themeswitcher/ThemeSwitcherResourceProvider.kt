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

import androidx.annotation.ArrayRes
import androidx.annotation.StyleableRes
import com.forcetower.uefs.R

class ThemeSwitcherResourceProvider {
    @get:ArrayRes
    val primaryColors: Int
        get() = R.array.material_primary_palettes

    @get:ArrayRes
    val secondaryColors: Int
        get() = R.array.material_secondary_palettes

    @get:ArrayRes
    val backgroundColors: Int
        get() = R.array.material_background_palettes

    @get:ArrayRes
    val primaryColorsContentDescription: Int
        get() = R.array.material_palettes_content_description

    @get:ArrayRes
    val secondaryColorsContentDescription: Int
        get() = R.array.material_palettes_content_description

    @get:ArrayRes
    val backgroundColorsContentDescription: Int
        get() = R.array.material_background_content_description

    @get:StyleableRes
    val primaryThemeOverlayAttrs: IntArray
        get() = PRIMARY_THEME_OVERLAY_ATTRS

    @get:StyleableRes
    val secondaryThemeOverlayAttrs: IntArray
        get() = SECONDARY_THEME_OVERLAY_ATTRS

    @get:StyleableRes
    val backgroundThemeOverlayAttrs: IntArray
        get() = BACKGROUND_THEME_OVERLAY_ATTRS

    companion object {
        @JvmStatic
        @StyleableRes
        private val PRIMARY_THEME_OVERLAY_ATTRS = intArrayOf(
            R.attr.colorPrimary,
            R.attr.colorPrimaryDark,
            R.attr.colorPrimaryLight,
            R.attr.colorPrimaryAlpha
        )

        @JvmStatic
        @StyleableRes
        private val SECONDARY_THEME_OVERLAY_ATTRS = intArrayOf(
            R.attr.colorAccent,
            R.attr.colorSecondary
        )

        @JvmStatic
        @StyleableRes
        private val BACKGROUND_THEME_OVERLAY_ATTRS = intArrayOf(
            R.attr.background,
            R.attr.colorStatusBar,
            R.attr.colorSurface,
            android.R.attr.windowBackground
        )
    }
}
