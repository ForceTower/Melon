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

package com.forcetower.core.utils

import android.graphics.Bitmap
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.core.graphics.ColorUtils
import androidx.core.math.MathUtils
import androidx.palette.graphics.Palette

object ColorUtils {
    const val IS_LIGHT = 0
    const val IS_DARK = 1
    const val LIGHTNESS_UNKNOWN = 2

    @CheckResult
    @ColorInt
    @JvmStatic
    fun modifyAlpha(@ColorInt color: Int, @IntRange(from = 0, to = 255) alpha: Int): Int {
        return color and 0x00ffffff or (alpha shl 24)
    }

    @CheckResult
    @ColorInt
    @JvmStatic
    fun modifyAlpha(
        @ColorInt color: Int,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float
    ): Int {
        return modifyAlpha(color, (255f * alpha).toInt())
    }

    fun isDark(@ColorInt color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    fun isDark(palette: Palette?): Int {
        val mostPopulous = getMostPopulousSwatch(palette) ?: return LIGHTNESS_UNKNOWN
        return if (isDark(mostPopulous.rgb)) IS_DARK else IS_LIGHT
    }

    fun isDark(
        bitmap: Bitmap,
        backupPixelX: Int,
        backupPixelY: Int
    ): Boolean { // first try palette with a small color quant size
        val palette = Palette.from(bitmap).maximumColorCount(3).generate()
        return if (palette.swatches.size > 0) {
            isDark(palette) == IS_DARK
        } else { // if palette failed, then check the color of the specified pixel
            isDark(bitmap.getPixel(backupPixelX, backupPixelY))
        }
    }

    fun getMostPopulousSwatch(palette: Palette?): Palette.Swatch? {
        var mostPopulous: Palette.Swatch? = null
        if (palette != null) {
            for (swatch in palette.swatches) {
                if (mostPopulous == null || swatch.population > mostPopulous.population) {
                    mostPopulous = swatch
                }
            }
        }
        return mostPopulous
    }

    @ColorInt
    fun scrimify(
        @ColorInt color: Int,
        isDark: Boolean,
        @FloatRange(from = 0.0, to = 1.0) multiplier: Float
    ): Int {
        var lightnessMultiplier = multiplier
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        if (!isDark) {
            lightnessMultiplier += 1f
        } else {
            lightnessMultiplier = 1f - lightnessMultiplier
        }
        hsl[2] = MathUtils.clamp(hsl[2] * lightnessMultiplier, 0f, 1f)
        return ColorUtils.HSLToColor(hsl)
    }
}
