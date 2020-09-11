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

package com.forcetower.core.transition

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.transition.Transition
import android.transition.TransitionValues
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import com.forcetower.core.R
import com.forcetower.core.extensions.isDarkTheme

/**
 * A transition that animates the RGB scale of an [ImageView]s `drawable` when in dark mode.
 */
class DarkenImage(context: Context, attrs: AttributeSet) : Transition(context, attrs) {

    private val isDarkTheme = context.isDarkTheme
    private val initialRgbScale: Float
    private val finalRgbScale: Float

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DarkenImage)
        initialRgbScale = ta.getFloat(R.styleable.DarkenImage_initialRgbScale, 1.0f)
        finalRgbScale = ta.getFloat(R.styleable.DarkenImage_finalRgbScale, 1.0f)
        ta.recycle()
    }

    override fun captureStartValues(transitionValues: TransitionValues?) { }

    override fun captureEndValues(transitionValues: TransitionValues?) { }

    override fun createAnimator(
        sceneRoot: ViewGroup?,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (!isDarkTheme) return null
        if (initialRgbScale == finalRgbScale) return null
        val iv = endValues?.view as? ImageView ?: return null
        val drawable = iv.drawable ?: return null
        return ValueAnimator.ofFloat(initialRgbScale, finalRgbScale).apply {
            addUpdateListener { listener ->
                val cm = ColorMatrix()
                val rgbScale = listener.animatedValue as Float
                cm.setScale(rgbScale, rgbScale, rgbScale, 1.0f)
                drawable.colorFilter = ColorMatrixColorFilter(cm)
            }
        }
    }
}
