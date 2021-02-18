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

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.Keep
import com.forcetower.uefs.R
import kotlin.math.min
import kotlin.math.roundToInt

class CircleProgressBar(ctx: Context, private val attrs: AttributeSet) : View(ctx, attrs) {
    private var foregroundStrokeWidth = 4f
    private var backgroundStrokeWidth = 4f
    private var progress = 0f
    private var min: Int = 0
    private var max: Int = 100
    private var factor = 0.3f

    private val startAngle = -90
    private var color = Color.DKGRAY
    private lateinit var rectF: RectF
    private lateinit var backgroundPaint: Paint
    private lateinit var foregroundPaint: Paint

    init {
        init()
    }

    private fun init() {
        rectF = RectF()
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircleProgressBar,
            0,
            0
        )

        try {
            foregroundStrokeWidth = typedArray.getDimension(R.styleable.CircleProgressBar_foregroundThickness, foregroundStrokeWidth)
            backgroundStrokeWidth = typedArray.getDimension(R.styleable.CircleProgressBar_backgroundThickness, backgroundStrokeWidth)
            progress = typedArray.getFloat(R.styleable.CircleProgressBar_progress, progress)
            color = typedArray.getInt(R.styleable.CircleProgressBar_progressbarColor, color)
            min = typedArray.getInt(R.styleable.CircleProgressBar_min, min)
            max = typedArray.getInt(R.styleable.CircleProgressBar_max, max)
            factor = typedArray.getFloat(R.styleable.CircleProgressBar_backgroundAlpha, factor)
        } finally {
            typedArray.recycle()
        }

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = adjustAlpha(this@CircleProgressBar.color, factor)
            style = Paint.Style.STROKE
            strokeWidth = this@CircleProgressBar.backgroundStrokeWidth
        }

        foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = this@CircleProgressBar.color
            style = Paint.Style.STROKE
            strokeWidth = this@CircleProgressBar.foregroundStrokeWidth
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).roundToInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val min = min(height, width)

        setMeasuredDimension(min, min)
        val fl = foregroundStrokeWidth / 2
        rectF.set(fl, fl, min - fl, min - fl)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawOval(rectF, backgroundPaint)
        val angle = 360 * progress / max
        canvas.drawArc(rectF, startAngle.toFloat(), angle, false, foregroundPaint)
    }

    @Keep
    fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }

    @Keep
    fun setProgressInt(progress: Int) {
        this.progress = progress.toFloat()
        invalidate()
    }

    @Keep
    fun setMax(max: Int) {
        this.max = max
        invalidate()
    }

    @Keep
    fun setProgressWithAnimation(progress: Float) {
        val objectAnimator = ObjectAnimator.ofFloat(this, "progress", this@CircleProgressBar.progress, progress)
        objectAnimator.duration = 1500
        objectAnimator.interpolator = DecelerateInterpolator()
        objectAnimator.start()
    }

    @Keep
    fun setProgressWithAnimationInt(progressInt: Int) {
        setProgressWithAnimation(progressInt.toFloat())
    }
}
