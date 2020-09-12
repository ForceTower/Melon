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

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.ViewSwitcher
import androidx.annotation.StyleRes
import androidx.core.widget.TextViewCompat

internal class TextViewFactory(
    val context: Context,
    @StyleRes val styleId: Int,
    val center: Boolean = false
) : ViewSwitcher.ViewFactory {

    override fun makeView(): View {
        val textView = TextView(context)

        if (center) textView.gravity = Gravity.CENTER
        textView.maxLines = 1

        TextViewCompat.setTextAppearance(textView, styleId)
        return textView
    }
}
