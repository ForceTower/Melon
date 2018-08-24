/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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