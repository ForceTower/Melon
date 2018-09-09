/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.feature.shared

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import com.forcetower.uefs.R
import com.google.android.material.snackbar.Snackbar

fun Snackbar.config(bottomPadding: Int = 12) {
    val context = view.context
    val params = view.layoutParams as ViewGroup.MarginLayoutParams

    val px12 = getPixelsFromDp(context, 12)
    val px6  = getPixelsFromDp(context, 6)
    val pxB  = getPixelsFromDp(context, bottomPadding)

    params.setMargins(px12, px12, px12, pxB)
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