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
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Rect
import android.transition.ChangeBounds
import android.transition.TransitionValues
import android.util.AttributeSet
import android.view.ViewGroup
import com.forcetower.core.widget.ParallaxScrimageView

/**
 * An extension to [ChangeBounds] designed to work with [ParallaxScrimageView]. This
 * will remove any parallax applied while also performing a `ChangeBounds` transition.
 */
class DeparallaxChangeBounds(
    context: Context?,
    attrs: AttributeSet?
) : ChangeBounds(context, attrs) {
    override fun captureEndValues(transitionValues: TransitionValues) {
        super.captureEndValues(transitionValues)

        if (transitionValues.view !is ParallaxScrimageView) return
        val psv: ParallaxScrimageView = transitionValues.view as ParallaxScrimageView

        if (psv.offset == 0) return
        val bounds = transitionValues.values[PROPNAME_BOUNDS] as Rect
        bounds.offset(0, psv.offset)
        transitionValues.values[PROPNAME_BOUNDS] = bounds
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator {
        val changeBounds = super.createAnimator(sceneRoot, startValues, endValues)
        if (startValues == null || endValues == null || endValues.view !is ParallaxScrimageView) return changeBounds

        val psv: ParallaxScrimageView = endValues.view as ParallaxScrimageView
        if (psv.offset == 0) return changeBounds
        val deparallax = ObjectAnimator.ofInt(psv, ParallaxScrimageView.OFFSET, 0)
        val transition = AnimatorSet()
        transition.playTogether(changeBounds, deparallax)
        return changeBounds
    }

    companion object {
        private const val PROPNAME_BOUNDS = "android:changeBounds:bounds"
    }
}
