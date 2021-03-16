/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.core.utils.AnimUtils.lerp
import com.forcetower.uefs.R
import kotlin.math.max
import kotlin.math.min

class BubbleDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val insetHorizontal: Float
    private val insetVertical: Float

    private val currentRect = RectF()
    private val previousRect = RectF()
    private val temp = RectF() // to avoid object allocations

    private var animator: ValueAnimator? = null
    private var pendingAnimation = false

    var userScrolled = false

    private var progress = 1f

    var bubbleRange: IntRange = -1..-1
        set(value) {
            field = value
            pendingAnimation = true
        }

    init {
        val attrs = context.obtainStyledAttributes(
            R.style.Widget_UTheme_BubbleIndicatorDecoration,
            R.styleable.BubbleIndicatorDecoration
        )
        paint.color = attrs.getColor(R.styleable.BubbleIndicatorDecoration_android_color, 0)
        insetHorizontal = attrs.getDimension(R.styleable.BubbleIndicatorDecoration_insetHorizontal, 0f)
        insetVertical = attrs.getDimension(R.styleable.BubbleIndicatorDecoration_insetVertical, 0f)
        attrs.recycle()
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (pendingAnimation) {
            pendingAnimation = false

            animator?.cancel()

            computeTargetRect(parent, state, bubbleRange, temp)
            previousRect.set(currentRect)
            currentRect.set(temp)

            startAnimatorIfNeeded(previousRect, currentRect, parent)
        } else if (userScrolled) {
            userScrolled = false
            animator?.cancel()

            computeTargetRect(parent, state, bubbleRange, temp)
            previousRect.set(currentRect)
            currentRect.set(temp)
            progress = 1f
            parent.invalidateItemDecorations()
        }

        val rect = getDrawingRect(previousRect, currentRect)
        drawBubble(rect, canvas)
    }

    private fun getDrawingRect(initial: RectF, target: RectF): RectF {
        when {
            initial.isEmpty -> computeScalingRect(target, progress)
            target.isEmpty -> computeScalingRect(initial, progress)
            else -> computeMovingRect(initial, target, progress)
        }
        return temp
    }

    private fun computeScalingRect(rect: RectF, progress: Float) {
        // Create the effect of growing/shrinking from the center.
        val dx = rect.width() * progress / 2
        val dy = rect.height() * progress / 2
        temp.set(
            rect.centerX() - dx,
            rect.centerY() - dy,
            rect.centerX() + dx,
            rect.centerY() + dy
        )
    }

    private fun computeMovingRect(initial: RectF, target: RectF, progress: Float) {
        temp.set(
            lerp(initial.left, target.left, progress),
            lerp(initial.top, target.top, progress),
            lerp(initial.right, target.right, progress),
            lerp(initial.bottom, target.bottom, progress)
        )
    }

    private fun drawBubble(rect: RectF, canvas: Canvas) {
        if (rect.isEmpty) return

        val radius = min(rect.width(), rect.height()) / 2
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun computeTargetRect(
        parent: RecyclerView,
        state: RecyclerView.State,
        range: IntRange,
        outRectF: RectF
    ) {
        if (state.itemCount < 1 || range.isEmpty()) {
            outRectF.setEmpty()
            return
        }

        var minLeft = parent.width.toFloat()
        var minTop = parent.height.toFloat()
        var maxRight = 0f
        var maxBottom = 0f

        val seenPositions = hashSetOf<Int>()
        parent.forEach { view ->
            val position = parent.getChildViewHolder(view).adapterPosition
            if (position != -1 && position in range && seenPositions.add(position)) {
                minLeft = min(minLeft, view.left.toFloat())
                minTop = min(minTop, view.top.toFloat())
                maxRight = max(maxRight, view.right.toFloat())
                maxBottom = max(maxBottom, view.bottom.toFloat())
            }
        }

        outRectF.set(minLeft, minTop, maxRight, maxBottom)
        outRectF.inset(insetHorizontal, insetVertical)
    }

    private fun startAnimatorIfNeeded(initial: RectF, target: RectF, parent: RecyclerView) {
        if ((initial.isEmpty && target.isEmpty) || initial == target) {
            return
        }

        animator = if (target.isEmpty) {
            ValueAnimator.ofFloat(1f, 0f)
        } else {
            ValueAnimator.ofFloat(0f, 1f)
        }.apply {
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        animator = null
                    }
                }
            )
            addUpdateListener {
                progress = animatedValue as Float
                parent.invalidateItemDecorations()
            }

            start()
        }
    }
}
