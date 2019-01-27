/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.feature.event

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.forcetower.uefs.feature.shared.extensions.formatDate
import com.forcetower.uefs.feature.shared.extensions.formatFullDate

@BindingAdapter("loadImage")
fun loadImage(iv: ImageView, url: String) {
    GlideApp.with(iv.context)
        .load(url)
        .placeholder(R.mipmap.ic_unes_large_image_512)
        .centerCrop()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(iv)
}

@BindingAdapter("eventStartDate")
fun eventStartDate(tv: TextView, value: Long) {
    val date = value.formatDate()
    tv.text = date
}

@BindingAdapter("eventStart")
fun eventStart(tv: TextView, value: Long) {
    val date = value.formatFullDate()
    val message = tv.context.getString(R.string.event_starts_at, date)
    tv.text = message
}

@BindingAdapter("eventPrice")
fun eventPrice(tv: TextView, value: Double?) {
    val context = tv.context
    if (value != null) {
        tv.text = value.toString()
    } else {
        tv.text = context.getString(R.string.event_free)
    }
}