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

package com.forcetower.uefs.feature.reminders

import android.graphics.Paint
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.uefs.feature.shared.extensions.formatSimpleDay

@BindingAdapter("strikeText")
fun strikeText(tv: TextView, strike: Boolean) {
    if (strike) tv.paintFlags = tv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    else tv.paintFlags = tv.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
}

@BindingAdapter("reminderDate")
fun reminderDate(tv: TextView, date: Long?) {
    if (date == null) return
    tv.text = date.formatSimpleDay()
}
