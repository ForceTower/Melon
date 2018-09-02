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

package com.forcetower.unes.core.util

import android.content.Context
import android.util.DisplayMetrics
import androidx.annotation.NonNull
import android.util.TypedValue
import androidx.databinding.adapters.TextViewBindingAdapter.setTextSize
import android.text.TextPaint



object ViewUtils {
    @JvmStatic
    fun getSingleLineTextSize(text: String,
                              paint: TextPaint,
                              targetWidth: Float,
                              low: Float,
                              high: Float,
                              precision: Float,
                              metrics: DisplayMetrics): Float {
        val mid = (low + high) / 2.0f

        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, mid, metrics)
        val maxLineWidth = paint.measureText(text)

        return when {
            high - low < precision -> low
            maxLineWidth > targetWidth -> getSingleLineTextSize(text, paint, targetWidth, low, mid, precision, metrics)
            maxLineWidth < targetWidth -> getSingleLineTextSize(text, paint, targetWidth, mid, high, precision, metrics)
            else -> mid
        }
    }
}