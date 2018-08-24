/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
