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

package com.forcetower.uefs.feature.shared.extensions

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.uefs.R
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
            val minutes = TimeUnit.MINUTES.convert(diff - (hours * oneHor), TimeUnit.MILLISECONDS)
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
    return when (this.toUpperCase(Locale.getDefault())) {
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

fun String.createTimeInt(): Int {
    return try {
        val split = this.split(":")
        val hour = split[0].toInt() * 60
        val minute = split[1].toInt()
        hour + minute
    } catch (t: Throwable) {
        Timber.e(t, "Failed to parse $this")
        0
    }
}

fun Long.toCalendar(): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    return calendar
}

fun Long.formatDateTime(): String {
    return if (this.isToday()) {
        this.formatTime()
    } else {
        this.formatDate()
    }
}

fun Long.isToday(): Boolean {
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val date = dateFormat.format(this)
    return date == dateFormat.format(System.currentTimeMillis())
}

fun Long.formatDate(): String {
    val dateFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())
    return dateFormat.format(this)
}

fun Long.formatTime(): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return dateFormat.format(this)
}

fun Long.formatTimeWithoutSeconds(): String {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return dateFormat.format(this)
}

fun Long.formatFullDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return format.format(date)
}

fun Long.formatSimpleDay(): String {
    val date = Date(this)
    val format = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
    return format.format(date)
}

fun Long.formatMonthYear(): String {
    val date = Date(this)
    val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return format.format(date)
}

fun String?.generateCalendarFromHour(): Calendar? {
    if (this == null) return null

    try {
        val calendar = Calendar.getInstance()
        val parts = trim { it <= ' ' }.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size != 1) {
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]))
            calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]))

            if (parts.size == 3)
                calendar.set(Calendar.SECOND, Integer.parseInt(parts[2]))

            return calendar
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return null
}
