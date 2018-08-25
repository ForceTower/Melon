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
import android.content.res.Resources
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.forcetower.unes.R
import com.forcetower.unes.core.util.ViewUtils


class CutoutTextView(ctx: Context, attrs: AttributeSet): View(ctx, attrs) {
    private val textPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var foregroundColor = Color.MAGENTA
    private var text: String = ""
    private val maxTextSize: Float = resources.getDimensionPixelSize(R.dimen.display_4_text_size).toFloat()
    private var textSize = 0F
    private var textX = 0F
    private var textY = 0F
    private var cutout: Bitmap? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CutoutTextView, 0, 0)
        if (a.hasValue(R.styleable.CutoutTextView_android_fontFamily)) {
            try {
                val font = ResourcesCompat.getFont(context, a.getResourceId(R.styleable.CutoutTextView_android_fontFamily, 0))
                if (font != null) textPaint.typeface = font
            } catch (nfe: Resources.NotFoundException){}
        }

        if (a.hasValue(R.styleable.CutoutTextView_foregroundColor)) {
            foregroundColor = a.getColor(R.styleable.CutoutTextView_foregroundColor, foregroundColor);
        }

        if (a.hasValue(R.styleable.CutoutTextView_android_text)) {
            text = a.getString(R.styleable.CutoutTextView_android_text)?: ""
        }

        a.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateTextPosition()
        createBitmap()
    }

    private fun calculateTextPosition() {
        val targetWidth = width / PHI
        textSize = ViewUtils.getSingleLineTextSize(text, textPaint, targetWidth, 0f, maxTextSize, 0.5f, resources.displayMetrics)
        textPaint.textSize = textSize
        textX = (width - textPaint.measureText(text)) / 2
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textHeight = textBounds.height()
        textY = (height + textHeight.toFloat()) / 2
    }

    private fun createBitmap() {
        if (cutout != null && !cutout!!.isRecycled) {
            cutout!!.recycle()
        }
        cutout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        cutout!!.setHasAlpha(true)
        val cutoutCanvas = Canvas(cutout!!)
        cutoutCanvas.drawColor(foregroundColor)

        // this is the magic – Clear mode punches out the bitmap
        textPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        cutoutCanvas.drawText(text, textX, textY, textPaint)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(cutout!!, 0F, 0F, null)
    }

    override fun hasOverlappingRendering(): Boolean = true

    companion object {
        const val PHI = 1.6182f
    }
}