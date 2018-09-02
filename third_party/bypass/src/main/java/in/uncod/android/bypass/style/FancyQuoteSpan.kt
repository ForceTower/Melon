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
import android.text.Layout
import android.text.style.LeadingMarginSpan

import androidx.annotation.ColorInt

/**
 * A quote span with a nicer presentation
 */
class FancyQuoteSpan(private val lineWidth: Int,
                     private val gapWidth: Int,
                     @param:ColorInt private val lineColor: Int) : LeadingMarginSpan {

    override fun getLeadingMargin(first: Boolean): Int {
        return lineWidth + gapWidth
    }

    override fun drawLeadingMargin(c: Canvas,
                                   p: Paint,
                                   x: Int,
                                   dir: Int,
                                   top: Int,
                                   baseline: Int,
                                   bottom: Int,
                                   text: CharSequence,
                                   start: Int,
                                   end: Int,
                                   first: Boolean,
                                   layout: Layout) {
        val prevStyle = p.style
        val prevColor = p.color
        p.style = Paint.Style.FILL
        p.color = lineColor
        c.drawRect(x.toFloat(), top.toFloat(), (x + dir * lineWidth).toFloat(), bottom.toFloat(), p)
        p.style = prevStyle
        p.color = prevColor
    }
}
