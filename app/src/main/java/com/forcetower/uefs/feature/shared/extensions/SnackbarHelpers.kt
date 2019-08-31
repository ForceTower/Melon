/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.feature.shared.extensions

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.forcetower.uefs.R
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.google.android.material.snackbar.Snackbar

fun Snackbar.config(bottomPadding: Int = 12, pxElevation: Int = 6) {
    val context = view.context
    val params = view.layoutParams as ViewGroup.MarginLayoutParams

    val px12 = getPixelsFromDp(context, 12).toInt()
    val px6 = getPixelsFromDp(context, pxElevation)
    val pxB = getPixelsFromDp(context, bottomPadding).toInt()

    params.setMargins(px12, px12, px12, pxB)
    view.elevation = px6
    view.bringToFront()

    view.layoutParams = params

    view.background = context.getDrawable(R.drawable.snackbar_background)

    val colorOnSurface = ContextCompat.getColor(context, R.color.white)

    val font = ResourcesCompat.getFont(context, R.font.product_sans_regular)
    val tv = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    tv.typeface = font
    tv.setTextColor(colorOnSurface)

    try {
        val at = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
        at.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
        at.typeface = font
    } catch (ignored: Exception) {}
}