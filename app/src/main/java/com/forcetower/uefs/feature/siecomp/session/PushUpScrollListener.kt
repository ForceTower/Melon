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

package com.forcetower.uefs.feature.siecomp.session

import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView

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
        pushPointY = if (title.visibility == View.VISIBLE) {
            // If title is in header, push the up button from the first line of text.
            // Due to using auto-sizing text, the view needs to be a fixed height (not wrap)
            // with gravity bottom so we find the text top using the baseline.
            val textTop = title.baseline - title.textSize.toInt()
            textTop - up.height
        } else {
            // If no title in header, push the up button based on the bottom of the photo
            val photo = recyclerView.findViewById<View>(imageResId)
            photo.height - up.height
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (pushPointY < 0) return
        val scrollY =
            recyclerView.findViewHolderForAdapterPosition(0)?.itemView?.top ?: Integer.MIN_VALUE

        val desiredTop = Math.min(pushPointY + scrollY, 0)
        if (desiredTop != up.top) {
            val offset = desiredTop - up.top
            up.offsetTopAndBottom(offset)
        }
    }
}
