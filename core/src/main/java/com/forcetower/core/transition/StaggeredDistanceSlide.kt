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
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.transition.TransitionValues
import android.transition.Visibility
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.core.content.withStyledAttributes
import com.forcetower.core.R
import com.forcetower.core.utils.TransitionUtils

/**
 * An alternative to [android.transition.Slide] which staggers elements by **distance**
 * rather than using start delays. That is elements start from/end at a progressively increasing
 * displacement such that they come together/move apart over the same duration as they enter/exit.
 * This can produce more cohesive choreography. The displacement factor can be controlled by the
 * `spread` attribute.
 *
 *
 * Currently only supports entering/exiting from the bottom edge.
 */
class StaggeredDistanceSlide : Visibility {

    private val spread: Int

    constructor() {
        spread = 1
    }

    @Keep
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        var tmpSpread = 1
        context.withStyledAttributes(attrs, R.styleable.StaggeredDistanceSlide) {
            tmpSpread = getInteger(R.styleable.StaggeredDistanceSlide_spread, 1)
        }
        spread = tmpSpread
    }

    override fun onAppear(
        sceneRoot: ViewGroup,
        view: View,
        startValues: TransitionValues,
        endValues: TransitionValues
    ): Animator {
        val position = endValues.values[PROPNAME_SCREEN_LOCATION] as IntArray
        return createAnimator(view, (sceneRoot.height + position[1] * spread).toFloat(), 0f)
    }

    override fun onDisappear(
        sceneRoot: ViewGroup,
        view: View,
        startValues: TransitionValues,
        endValues: TransitionValues
    ): Animator {
        val position = endValues.values[PROPNAME_SCREEN_LOCATION] as IntArray
        return createAnimator(view, 0f, (sceneRoot.height + position[1] * spread).toFloat())
    }

    private fun createAnimator(
        view: View,
        startTranslationY: Float,
        endTranslationY: Float
    ): Animator {
        view.translationY = startTranslationY
        val ancestralClipping = TransitionUtils.setAncestralClipping(view, false)
        val transition = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, endTranslationY)
        transition.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    TransitionUtils.restoreAncestralClipping(view, ancestralClipping.toMutableList())
                }
            }
        )
        return transition
    }

    companion object {

        private const val PROPNAME_SCREEN_LOCATION = "android:visibility:screenLocation"
    }
}
