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
import kotlin.math.floor

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
        val verticalSteps = floor((bounds.height() / gridSize).toDouble()).toInt()
        val horizontalSteps = floor((bounds.width() / gridSize).toDouble()).toInt()

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
