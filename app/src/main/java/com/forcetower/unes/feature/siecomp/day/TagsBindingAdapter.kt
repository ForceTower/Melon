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

package com.forcetower.unes.feature.siecomp.day

import android.content.Context
import android.graphics.Color
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.unes.R
import com.forcetower.unes.core.model.event.Tag
import com.forcetower.unes.core.util.ColorUtils
import timber.log.Timber

@BindingAdapter("sessionTags")
fun sessionTags(recyclerView: RecyclerView, sessionTags: List<Tag>?) {
    recyclerView.adapter = (recyclerView.adapter as? TagsAdapter ?: TagsAdapter())
        .apply {
            submitList(sessionTags ?: emptyList())
        }
}

@BindingAdapter("tagTint")
fun tagTint(textView: TextView, color: Int) {
    // Tint the colored dot
    (textView.compoundDrawablesRelative[0]?.mutate() as? GradientDrawable)?.setColor(
            tagTintOrDefault(
                    color,
                    textView.context
            )
    )?: Timber.d("Some of them are null")
}

fun tagTintOrDefault(color: Int, context: Context): Int {
    return if (color != TRANSPARENT) {
        valueOf(color)
    } else {
        ContextCompat.getColor(context, R.color.default_tag_color)
    }
}

fun valueOf(color: Int): Int {
    return ColorUtils.modifyAlpha(color, 255)
}
