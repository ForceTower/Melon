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