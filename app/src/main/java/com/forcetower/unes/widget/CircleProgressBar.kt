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

package com.forcetower.unes.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.forcetower.unes.R
import kotlin.math.min
import android.view.animation.DecelerateInterpolator
import android.animation.ObjectAnimator




class CircleProgressBar(ctx: Context, private val attrs: AttributeSet): View(ctx, attrs) {
    private var foregroundStrokeWidth = 4f
    private var backgroundStrokeWidth = 4f
    private var progress = 0f
    private var min = 0
    private var max = 100

    private val startAngle = -90
    private var color = Color.DKGRAY
    private var rectF: RectF? = null
    private var backgroundPaint: Paint? = null
    private var foregroundPaint: Paint? = null

    init {
        init()
    }

    private fun init() {
        rectF = RectF()
        val typedArray = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.CircleProgressBar,
                0, 0)

        try {
            foregroundStrokeWidth = typedArray.getDimension(R.styleable.CircleProgressBar_foregroundThickness, foregroundStrokeWidth)
            backgroundStrokeWidth = typedArray.getDimension(R.styleable.CircleProgressBar_backgroundThickness, backgroundStrokeWidth)
            progress = typedArray.getFloat(R.styleable.CircleProgressBar_progress, progress)
            color = typedArray.getInt(R.styleable.CircleProgressBar_progressbarColor, color)
            min = typedArray.getInt(R.styleable.CircleProgressBar_min, min)
            max = typedArray.getInt(R.styleable.CircleProgressBar_max, max)
        } finally {
            typedArray.recycle()
        }

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = adjustAlpha(this@CircleProgressBar.color, 0.3f)
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
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width  = getDefaultSize(suggestedMinimumWidth,  widthMeasureSpec)
        val min    = min(height, width)

        setMeasuredDimension(min, min)
        val fl = foregroundStrokeWidth / 2
        rectF!!.set(fl, fl, min - fl, min - fl)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawOval(rectF!!, backgroundPaint!!)
        val angle = 360 * progress / max
        canvas.drawArc(rectF!!, startAngle.toFloat(), angle, false, foregroundPaint!!)

    }

    fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }

    fun setProgressWithAnimation(progress: Float) {
        val objectAnimator = ObjectAnimator.ofFloat(this, "progress", this@CircleProgressBar.progress, progress)
        objectAnimator.duration = 1500
        objectAnimator.interpolator = DecelerateInterpolator()
        objectAnimator.start()
    }
}