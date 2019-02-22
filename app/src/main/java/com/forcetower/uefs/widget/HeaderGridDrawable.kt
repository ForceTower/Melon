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

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import com.forcetower.uefs.R

/**
 * Custom [Drawable] for drawing a grid pattern. We use this rather than tiling a pattern in order
 * to have greater control; specifically we always want a horizontal grid line along the bottom of
 * the drawable.
 */
class HeaderGridDrawable(context: Context) : Drawable() {

    private val paint: Paint
    private val gridSize: Int
    private val halfStrokeWidth: Float

    init {
        val attrs = context.obtainStyledAttributes(
            R.style.Widget_Schedule_HeaderGrid,
            R.styleable.HeaderGridDrawable
        )
        paint = Paint().apply {
            color = attrs.getColorOrThrow(R.styleable.HeaderGridDrawable_android_color)
            strokeWidth = attrs.getDimensionOrThrow(R.styleable.HeaderGridDrawable_gridStrokeWidth)
        }
        halfStrokeWidth = paint.strokeWidth / 2f
        gridSize = attrs.getDimensionPixelSizeOrThrow(R.styleable.HeaderGridDrawable_gridSize)
        attrs.recycle()
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val height = bounds.height().toFloat()
        if (height == 0f) return

        val left = bounds.left.toFloat()
        val top = bounds.top.toFloat()
        val right = bounds.right.toFloat()
        val bottom = bounds.bottom.toFloat()
        val verticalSteps = Math.floor((bounds.height() / gridSize).toDouble()).toInt()
        val horizontalSteps = Math.floor((bounds.width() / gridSize).toDouble()).toInt()

        // Always draw a hz line at the very top.
        val lines = mutableListOf(left, top + halfStrokeWidth, right, top + halfStrokeWidth)

        // Draw hz lines from bottom to top
        for (i in 0..verticalSteps) {
            val y = height - halfStrokeWidth - (gridSize * i).toFloat()
            lines += left
            lines += y
            lines += right
            lines += y
        }

        for (i in 0..horizontalSteps) {
            val x = (gridSize * i).toFloat() - halfStrokeWidth
            lines += x
            lines += top
            lines += x
            lines += bottom
        }

        canvas.drawLines(lines.toFloatArray(), paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }
}
