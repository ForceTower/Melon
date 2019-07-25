/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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