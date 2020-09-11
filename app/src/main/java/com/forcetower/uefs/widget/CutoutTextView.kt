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
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.forcetower.core.utils.ViewUtils
import com.forcetower.uefs.R

class CutoutTextView(ctx: Context, attrs: AttributeSet) : View(ctx, attrs) {
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
            } catch (nfe: Resources.NotFoundException) {}
        }

        if (a.hasValue(R.styleable.CutoutTextView_foregroundColor)) {
            foregroundColor = a.getColor(R.styleable.CutoutTextView_foregroundColor, foregroundColor)
        }

        if (a.hasValue(R.styleable.CutoutTextView_android_text)) {
            text = a.getString(R.styleable.CutoutTextView_android_text) ?: ""
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
