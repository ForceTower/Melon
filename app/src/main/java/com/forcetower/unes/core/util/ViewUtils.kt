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