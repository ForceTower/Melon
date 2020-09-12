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
