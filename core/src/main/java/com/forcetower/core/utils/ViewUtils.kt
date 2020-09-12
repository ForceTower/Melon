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
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.text.TextPaint
import android.util.DisplayMetrics
import android.util.Property
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.os.BuildCompat
import androidx.palette.graphics.Palette
import com.forcetower.core.utils.ColorUtils.modifyAlpha

object ViewUtils {
    val DRAWABLE_ALPHA: Property<Drawable, Int> =
        AnimUtils.createIntProperty(
            object : AnimUtils.IntProp<Drawable>("alpha") {
                override operator fun set(`object`: Drawable, value: Int) {
                    `object`.alpha = value
                }

                override operator fun get(`object`: Drawable): Int {
                    return `object`.alpha
                }
            }
        )

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

    @Suppress("DEPRECATION")
    @JvmStatic
    fun setLightStatusBar(view: View) {
        if (BuildCompat.isAtLeastR()) {
            view.windowInsetsController?.setSystemBarsAppearance(APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags: Int = view.systemUiVisibility
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            view.systemUiVisibility = flags
        }
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun setDarkStatusBar(view: View) {
        if (BuildCompat.isAtLeastR()) {
            view.windowInsetsController?.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags: Int = view.systemUiVisibility
            flags = flags and (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR).inv()
            view.systemUiVisibility = flags
        }
    }

    fun createRipple(
        palette: Palette?,
        @FloatRange(from = 0.0, to = 1.0) darkAlpha: Float,
        @FloatRange(from = 0.0, to = 1.0) lightAlpha: Float,
        @ColorInt fallbackColor: Int,
        bounded: Boolean
    ): RippleDrawable? {
        var rippleColor = fallbackColor
        if (palette != null) { // try the named swatches in preference order
            when {
                palette.vibrantSwatch != null -> {
                    rippleColor = modifyAlpha(
                        palette.vibrantSwatch!!.rgb,
                        darkAlpha
                    )
                }
                palette.lightVibrantSwatch != null -> {
                    rippleColor = modifyAlpha(
                        palette.lightVibrantSwatch!!.rgb,
                        lightAlpha
                    )
                }
                palette.darkVibrantSwatch != null -> {
                    rippleColor = modifyAlpha(
                        palette.darkVibrantSwatch!!.rgb,
                        darkAlpha
                    )
                }
                palette.mutedSwatch != null -> {
                    rippleColor = modifyAlpha(
                        palette.mutedSwatch!!.rgb,
                        darkAlpha
                    )
                }
                palette.lightMutedSwatch != null -> {
                    rippleColor = modifyAlpha(
                        palette.lightMutedSwatch!!.rgb,
                        lightAlpha
                    )
                }
                palette.darkMutedSwatch != null -> {
                    rippleColor = modifyAlpha(
                        palette.darkMutedSwatch!!.rgb,
                        darkAlpha
                    )
                }
            }
        }
        return RippleDrawable(
            ColorStateList.valueOf(rippleColor),
            null,
            if (bounded) ColorDrawable(Color.WHITE) else null
        )
    }
}
