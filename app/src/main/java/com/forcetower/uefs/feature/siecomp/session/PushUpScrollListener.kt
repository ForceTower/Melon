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

package com.forcetower.uefs.feature.siecomp.session

import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

/**
 * A [RecyclerView.OnScrollListener] which adjusts the position of the [up] view based on scroll.
 */
class PushUpScrollListener(
    private val up: View,
    recyclerView: View,
    @IdRes titleResId: Int,
    @IdRes imageResId: Int
) : RecyclerView.OnScrollListener() {

    private var pushPointY = -1

    init {
        val title = recyclerView.findViewById<TextView>(titleResId)
        pushPointY = if (title?.visibility == View.VISIBLE) {
            // If title is in header, push the up button from the first line of text.
            // Due to using auto-sizing text, the view needs to be a fixed height (not wrap)
            // with gravity bottom so we find the text top using the baseline.
            val textTop = title.baseline - title.textSize.toInt()
            textTop - up.height
        } else {
            // If no title in header, push the up button based on the bottom of the photo
            val photo = recyclerView.findViewById<View>(imageResId)
            (photo?.height ?: 0) - up.height
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (pushPointY < 0) return
        val scrollY =
            recyclerView.findViewHolderForAdapterPosition(0)?.itemView?.top ?: Integer.MIN_VALUE

        val desiredTop = min(pushPointY + scrollY, 0)
        if (desiredTop != up.top) {
            val offset = desiredTop - up.top
            up.offsetTopAndBottom(offset)
        }
    }
}
