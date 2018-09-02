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

package com.forcetower.unes.feature.shared

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.unes.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@BindingAdapter(value = ["timestamped"])
fun getTimeStampedDate(view: TextView, time: Long) {
    val context = view.context
    val now = System.currentTimeMillis()
    val diff = now - time

    val oneDay = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)
    val oneHor = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)
    val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
    val value = when {
        days > 1L -> {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val str = format.format(Date(time))
            context.getString(R.string.message_received_date_format, str)
        }
        days == 1L -> {
            val hours = TimeUnit.HOURS.convert(diff - oneDay, TimeUnit.MILLISECONDS)
            val str = days.toString() + "d " + hours.toString() + "h"
            context.getString(R.string.message_received_date_ago_format, str)
        }
        else -> {
            val hours = TimeUnit.HOURS.convert(diff, TimeUnit.MILLISECONDS)
            val minutes = TimeUnit.MINUTES.convert(diff - (hours*oneHor), TimeUnit.MILLISECONDS)
            val str = hours.toString() + "h " + minutes + "min"
            context.getString(R.string.message_received_date_ago_format, str)
        }
    }
    view.text = value
}

fun Int.toLongWeekDay(): String {
    return when (this) {
        1 -> "Domingo"
        2 -> "Segunda"
        3 -> "Terça"
        4 -> "Quarta"
        5 -> "Quinta"
        6 -> "Sexta"
        7 -> "Sábado"
        else -> "UNDEFINED"
    }
}

fun Int.toWeekDay(): String {
    return when (this) {
        1 -> "DOM"
        2 -> "SEG"
        3 -> "TER"
        4 -> "QUA"
        5 -> "QUI"
        6 -> "SEX"
        7 -> "SAB"
        else -> "UNDEFINED"
    }
}

fun String.fromWeekDay(): Int {
    return when(this.toUpperCase()) {
        "DOM" -> 1
        "SEG" -> 2
        "TER" -> 3
        "QUA" -> 4
        "QUI" -> 5
        "SEX" -> 6
        "SAB" -> 7
        else -> 0
    }
}