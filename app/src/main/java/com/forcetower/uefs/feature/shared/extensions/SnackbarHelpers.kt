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

package com.forcetower.uefs.feature.shared.extensions

import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.forcetower.core.utils.ViewUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.google.android.material.snackbar.Snackbar

fun Snackbar.config(pxElevation: Int = 6) {
    val context = view.context
    val px6 = getPixelsFromDp(context, pxElevation)

    view.elevation = px6

    val font = ResourcesCompat.getFont(context, R.font.product_sans_regular)
    val tv = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    tv.typeface = font

    try {
        val at = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
        at.setTextColor(ViewUtils.attributeColorUtils(context, R.attr.colorPrimary))
        at.isAllCaps = false
        at.typeface = font
    } catch (ignored: Exception) {}
}
