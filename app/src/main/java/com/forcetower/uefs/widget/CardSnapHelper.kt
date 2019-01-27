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

package com.forcetower.uefs.widget

import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.ramotion.cardslider.CardSliderLayoutManager
import java.security.InvalidParameterException

class CardSnapHelper : LinearSnapHelper() {

    private var recyclerView: RecyclerView? = null

    /**
     * Attaches the [CardSnapHelper] to the provided RecyclerView, by calling
     * [RecyclerView.setOnFlingListener].
     * You can call this method with `null` to detach it from the current RecyclerView.
     *
     * @param recyclerView The RecyclerView instance to which you want to add this helper or
     * `null` if you want to remove SnapHelper from the current
     * RecyclerView.
     *
     * @throws IllegalArgumentException if there is already a [RecyclerView.OnFlingListener]
     * attached to the provided [RecyclerView].
     *
     * @throws InvalidParameterException if provided RecyclerView has LayoutManager which is not
     * instance of CardSliderLayoutManager
     */
    @Throws(IllegalStateException::class)
    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        super.attachToRecyclerView(recyclerView)

        if (recyclerView != null && recyclerView.layoutManager !is CardSliderLayoutManager) {
            throw InvalidParameterException("LayoutManager must be instance of CardSliderLayoutManager")
        }

        this.recyclerView = recyclerView
    }

    override fun findTargetSnapPosition(layoutManager: RecyclerView.LayoutManager?, velocityX: Int, velocityY: Int): Int {
        val lm = layoutManager as CardSliderLayoutManager?

        val itemCount = lm!!.itemCount
        if (itemCount == 0) {
            return RecyclerView.NO_POSITION
        }

        val vectorProvider = layoutManager as RecyclerView.SmoothScroller.ScrollVectorProvider?

        val vectorForEnd = vectorProvider!!.computeScrollVectorForPosition(itemCount - 1)
                ?: return RecyclerView.NO_POSITION

        val distance = calculateScrollDistance(velocityX, velocityY)[0]
        var deltaJump: Int

        if (distance > 0) {
            deltaJump = Math.floor((distance / lm.cardWidth).toDouble()).toInt()
        } else {
            deltaJump = Math.ceil((distance / lm.cardWidth).toDouble()).toInt()
        }

        val deltaSign = Integer.signum(deltaJump)
        deltaJump = deltaSign * Math.min(3, Math.abs(deltaJump))

        if (vectorForEnd.x < 0) {
            deltaJump = -deltaJump
        }

        if (deltaJump == 0) {
            return RecyclerView.NO_POSITION
        }

        val currentPosition = lm.activeCardPosition
        if (currentPosition == RecyclerView.NO_POSITION) {
            return RecyclerView.NO_POSITION
        }

        var targetPos = currentPosition + deltaJump
        if (targetPos < 0 || targetPos >= itemCount) {
            targetPos = RecyclerView.NO_POSITION
        }

        return targetPos
    }

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        return (layoutManager as CardSliderLayoutManager).topView
    }

    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray? {
        val lm = layoutManager as CardSliderLayoutManager
        val viewLeft = lm.getDecoratedLeft(targetView)
        val activeCardLeft = lm.activeCardLeft
        val activeCardCenter = lm.activeCardLeft + lm.cardWidth / 2
        val activeCardRight = lm.activeCardLeft + lm.cardWidth

        val out = intArrayOf(0, 0)
        if (viewLeft < activeCardCenter) {
            val targetPos = lm.getPosition(targetView)
            val activeCardPos = lm.activeCardPosition
            if (targetPos != activeCardPos) {
                out[0] = -(activeCardPos - targetPos) * lm.cardWidth
            } else {
                out[0] = viewLeft - activeCardLeft
            }
        } else {
            out[0] = viewLeft - activeCardRight + 1
        }

        if (out[0] != 0) {
            recyclerView!!.smoothScrollBy(out[0], 0, AccelerateInterpolator())
        }

        return intArrayOf(0, 0)
    }

    override fun createScroller(layoutManager: RecyclerView.LayoutManager): LinearSmoothScroller? {
        return (layoutManager as CardSliderLayoutManager).getSmoothScroller(recyclerView!!)
    }
}
