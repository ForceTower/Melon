/*
 * Copyright (c) 2019.
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

import android.animation.ValueAnimator
import android.view.animation.AnimationUtils
import androidx.core.animation.addListener
import androidx.viewpager.widget.ViewPager

class ViewPagerPager(private val viewPager: ViewPager) {
    private val fastOutSlowIn =
        AnimationUtils.loadInterpolator(viewPager.context, android.R.interpolator.fast_out_slow_in)

    fun advance() {
        if (viewPager.width <= 0) return

        val current = viewPager.currentItem
        val next = ((current + 1) % requireNotNull(viewPager.adapter).count)
        val pages = next - current
        val dragDistance = pages * viewPager.width

        ValueAnimator.ofInt(0, dragDistance).apply {
            var dragProgress = 0
            var draggedPages = 0
            addListener(
                onStart = { viewPager.beginFakeDrag() },
                onEnd = { viewPager.endFakeDrag() }
            )
            addUpdateListener {
                if (!viewPager.isFakeDragging) {
                    // Sometimes onAnimationUpdate is called with initial value before
                    // onAnimationStart is called.
                    return@addUpdateListener
                }

                val dragPoint = (animatedValue as Int)
                val dragBy = dragPoint - dragProgress
                viewPager.fakeDragBy(-dragBy.toFloat())
                dragProgress = dragPoint

                // Fake dragging doesn't let you drag more than one page width. If we want to do
                // this then need to end and start a new fake drag.
                val draggedPagesProgress = dragProgress / viewPager.width
                if (draggedPagesProgress != draggedPages) {
                    viewPager.endFakeDrag()
                    viewPager.beginFakeDrag()
                    draggedPages = draggedPagesProgress
                }
            }
            duration = if (pages == 1) PAGE_CHANGE_DURATION else MULTI_PAGE_CHANGE_DURATION
            interpolator = fastOutSlowIn
        }.start()
    }

    companion object {
        private const val PAGE_CHANGE_DURATION = 400L
        private const val MULTI_PAGE_CHANGE_DURATION = 600L
    }
}