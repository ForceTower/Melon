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
import android.graphics.drawable.Drawable
import android.transition.TransitionValues
import android.transition.Visibility
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import com.forcetower.core.utils.ViewUtils

/**
 * A transition which fades in/out the background [Drawable] of a View.
 */
class BackgroundFade : Visibility {
    constructor() : super()
    @Keep
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onAppear(
        sceneRoot: ViewGroup?,
        view: View?,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (view == null || view.background == null) return null
        val background = view.background
        background.alpha = 0
        return ObjectAnimator.ofInt<Drawable>(background, ViewUtils.DRAWABLE_ALPHA, 0, 255)
    }

    override fun onDisappear(
        sceneRoot: ViewGroup,
        view: View?,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        return if (view == null || view.background == null) null else ObjectAnimator.ofInt<Drawable>(
            view.background,
            ViewUtils.DRAWABLE_ALPHA,
            0
        )
    }
}
