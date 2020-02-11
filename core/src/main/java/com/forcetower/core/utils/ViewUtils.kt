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

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.DisplayMetrics
import android.util.Property
import android.util.TypedValue
import androidx.annotation.AttrRes

object ViewUtils {
    val DRAWABLE_ALPHA: Property<Drawable, Int> =
        AnimUtils.createIntProperty(object : AnimUtils.IntProp<Drawable>("alpha") {
            override operator fun set(`object`: Drawable, value: Int) {
                `object`.alpha = value
            }

            override operator fun get(`object`: Drawable): Int {
                return `object`.alpha
            }
        })

    @JvmStatic
    fun getSingleLineTextSize(
        text: String,
        paint: TextPaint,
        targetWidth: Float,
        low: Float,
        high: Float,
        precision: Float,
        metrics: DisplayMetrics
    ): Float {
        val mid = (low + high) / 2.0f

        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, mid, metrics)
        val maxLineWidth = paint.measureText(text)

        return when {
            high - low < precision -> low
            maxLineWidth > targetWidth -> getSingleLineTextSize(text, paint, targetWidth, low, mid, precision, metrics)
            maxLineWidth < targetWidth -> getSingleLineTextSize(text, paint, targetWidth, mid, high, precision, metrics)
            else -> mid
        }
    }

    @JvmStatic
    fun attributeColorUtils(context: Context, @AttrRes attribute: Int): Int {
        val typedValue = context.obtainStyledAttributes(intArrayOf(attribute))
        val color = typedValue.getColor(0, 0)
        typedValue.recycle()
        return color
    }
}