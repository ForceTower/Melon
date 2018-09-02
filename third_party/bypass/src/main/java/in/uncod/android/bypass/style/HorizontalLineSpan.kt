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

package `in`.uncod.android.bypass.style

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

/**
 * Draws a line across the screen.
 */
class HorizontalLineSpan(color: Int, private val mLineHeight: Int, private val mTopBottomPadding: Int) : ReplacementSpan() {
    private val mPaint: Paint = Paint()

    init {
        mPaint.color = color
    }

    override fun getSize(
            paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        if (fm != null) {
            fm.ascent = -mLineHeight - mTopBottomPadding
            fm.descent = 0

            fm.top = fm.ascent
            fm.bottom = 0
        }

        // Take up *all* the horizontal space
        return Integer.MAX_VALUE
    }

    override fun draw(
            canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val middle = (top + bottom) / 2
        val halfLineHeight = mLineHeight / 2
        canvas.drawRect(x, (middle - halfLineHeight).toFloat(), Integer.MAX_VALUE.toFloat(), (middle + halfLineHeight).toFloat(), mPaint)
    }
}
