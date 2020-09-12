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

import android.content.Context
import android.graphics.Rect
import android.transition.ChangeBounds
import android.transition.TransitionValues
import android.util.AttributeSet
import android.view.View
import com.forcetower.core.R

/**
 * Shared element transitions do not seem to like transitioning from a single view to two separate
 * views so we need to alter the ChangeBounds transition to compensate
 */
class DetailedSharedEnter(
    context: Context,
    attrs: AttributeSet?
) : ChangeBounds(context, attrs) {
    private val heightDimension: Float
    private val widthDimension: Float

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DetailedSharedEnter)
        heightDimension = ta.getFloat(R.styleable.DetailedSharedEnter_heightDim, 4f)
        widthDimension = ta.getFloat(R.styleable.DetailedSharedEnter_widthDim, 3f)
        ta.recycle()
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        super.captureEndValues(transitionValues)
        val width = (transitionValues.values[PROPNAME_PARENT] as View).width
        val bounds = transitionValues.values[PROPNAME_BOUNDS] as Rect
        bounds.right = width
        bounds.bottom = (width * heightDimension / widthDimension).toInt()
        transitionValues.values[PROPNAME_BOUNDS] = bounds
    }

    companion object {
        private const val PROPNAME_BOUNDS = "android:changeBounds:bounds"
        private const val PROPNAME_PARENT = "android:changeBounds:parent"
    }
}
