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
import android.animation.ObjectAnimator
import android.content.Context
import android.transition.Transition
import android.transition.TransitionValues
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.forcetower.core.R

/**
 * A transition that animates the elevation of a View from a given value down to zero.
 *
 *
 * Useful for creating parent↔child navigation transitions
 * (https://www.google.com/design/spec/patterns/navigational-transitions.html#navigational-transitions-parent-to-child)
 * when combined with a [android.transition.ChangeBounds] on a shared element.
 */
class LiftOff(context: Context, attrs: AttributeSet?) : Transition(context, attrs) {
    private val initialElevation: Float
    private val finalElevation: Float

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.LiftOff)
        initialElevation = ta.getDimension(R.styleable.LiftOff_initialElevation, 0f)
        finalElevation = ta.getDimension(R.styleable.LiftOff_finalElevation, 0f)
        ta.recycle()
    }

    override fun getTransitionProperties(): Array<String> {
        return transitionProps
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        transitionValues.values[PROPNAME_ELEVATION] = initialElevation
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        transitionValues.values[PROPNAME_ELEVATION] = finalElevation
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues,
        endValues: TransitionValues
    ): Animator {
        return ObjectAnimator.ofFloat(
            endValues.view,
            View.TRANSLATION_Z,
            initialElevation,
            finalElevation
        )
    }

    companion object {
        private const val PROPNAME_ELEVATION = "cubic:liftoff:elevation"
        private val transitionProps = arrayOf(PROPNAME_ELEVATION)
    }
}
