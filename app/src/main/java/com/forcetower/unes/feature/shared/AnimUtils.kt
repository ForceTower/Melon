/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.feature.shared

import android.view.View
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