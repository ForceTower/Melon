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

package com.forcetower.uefs.feature.siecomp.bindings

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.core.utils.ColorUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.siecomp.Tag
import com.forcetower.uefs.feature.siecomp.schedule.TagsAdapter
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
    ) ?: Timber.d("Some of them are null")
}

fun tagTintOrDefault(color: Int, context: Context): Int {
    return if (color != Color.TRANSPARENT) {
        valueOf(color)
    } else {
        ContextCompat.getColor(context, R.color.blue_accent)
    }
}

fun valueOf(color: Int): Int {
    return ColorUtils.modifyAlpha(color, 255)
}
