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

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import com.forcetower.unes.R
import com.google.android.material.snackbar.Snackbar

fun Snackbar.config() {
    val context = view.context
    val params = view.layoutParams as ViewGroup.MarginLayoutParams

    val px12 = getPixelsFromDp(context, 12)
    val px6  = getPixelsFromDp(context, 6)

    params.setMargins(px12, px12, px12, px12)
    view.elevation = px6.toFloat()

    view.layoutParams = params

    view.background = context.getDrawable(R.drawable.snackbar_background)

    val font = ResourcesCompat.getFont(context, R.font.product_sans_regular)
    val tv = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    tv.typeface = font

    try {
        val at = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
        at.typeface = font
    } catch (ignored: Exception) {}

    ViewCompat.setElevation(this.view, 6f)
}