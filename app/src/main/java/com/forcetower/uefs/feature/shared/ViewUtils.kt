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

package com.forcetower.uefs.feature.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

fun View.inflater(): LayoutInflater = LayoutInflater.from(context)

inline fun <reified T : ViewDataBinding> ViewGroup.inflate(@LayoutRes res: Int, attachToParent: Boolean = false): T {
    val inflater = inflater()
    return DataBindingUtil.inflate(inflater, res, this, attachToParent)
}

inline fun <reified T : ViewDataBinding> LayoutInflater.inflate(@LayoutRes res: Int): T {
    return DataBindingUtil.inflate(this, res, null, false)
}

inline fun <T : ViewDataBinding> T.executeBindingsAfter(block: T.() -> Unit) {
    block()
    executePendingBindings()
}

fun RecyclerView.clearDecorations() {
    if (itemDecorationCount > 0) {
        for (i in itemDecorationCount - 1 downTo 0) {
            removeItemDecorationAt(i)
        }
    }
}
