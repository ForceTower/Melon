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

package com.forcetower.uefs.feature.shared

import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.AnimationUtils

fun View.fadeIn() {
    if (visibility == VISIBLE) return
    val fade: Animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
    visibility = VISIBLE
    startAnimation(fade)
    requestLayout()
}

fun View.fadeOut() {
    if (visibility == INVISIBLE) return
    val fade: Animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
    visibility = INVISIBLE
    startAnimation(fade)
    requestLayout()
}

fun View.fadeOutGone() {
    if (visibility == GONE) return
    val fade: Animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
    fade.setAnimationListener(
        object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                visibility = GONE
            }

            override fun onAnimationStart(animation: Animation?) {}
        }
    )
    visibility = INVISIBLE
    startAnimation(fade)
    requestLayout()
}
