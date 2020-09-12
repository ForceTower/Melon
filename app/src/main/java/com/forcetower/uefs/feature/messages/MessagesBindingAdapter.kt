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

package com.forcetower.uefs.feature.messages

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.sagres.utils.WordUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.feature.shared.extensions.formatMonthYear
import com.forcetower.uefs.feature.shared.extensions.formatTimeWithoutSeconds
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@BindingAdapter("messageContent")
fun messageContent(tv: TextView, cont: String?) {
    val content = cont ?: ""
    val spannable = SpannableString(content)
    Linkify.addLinks(spannable, Linkify.WEB_URLS)
    tv.text = spannable
    tv.movementMethod = LinkMovementMethod.getInstance()
    tv.autoLinkMask = tv.autoLinkMask or Linkify.WEB_URLS
}

@BindingAdapter("disciplineText")
fun disciplineText(tv: TextView, message: Message?) {
    message ?: return
    var discipline = message.discipline
    if (discipline == null && message.senderProfile == 3) discipline = "Secretaria Acadêmica"

    val text = discipline ?: message.senderName
    val title = if (!message.html) {
        WordUtils.toTitleCase(text)
    } else {
        text
    }
    tv.text = title
}

@BindingAdapter(value = ["messageTimestamp"])
fun getTimeStampedDate(view: TextView, message: Message?) {
    message ?: return
    if (!message.html) {
        val time = message.timestamp
        val context = view.context
        val now = System.currentTimeMillis()
        val diff = now - time

        val oneDay = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)
        val oneHor = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)
        val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        val value = when {
            days > 1L -> {
                val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
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
    } else {
        view.text = message.dateString ?: "Horário desconhecido"
    }
}

@BindingAdapter("senderName")
fun senderText(tv: TextView, message: Message?) {
    message ?: return
    var discipline = message.discipline
    if (discipline == null && message.senderProfile == 3) discipline = "Secretaria Acadêmica"

    if (discipline == null) {
        tv.visibility = GONE
    } else {
        tv.visibility = VISIBLE
        val text = message.senderName
        val title = WordUtils.toTitleCase(text)
        tv.text = title ?: "?????"
    }
}

@BindingAdapter("dateNumberFromDate")
fun dateNumberFromDate(tv: TextView, date: Date?) {
    if (date == null) {
        tv.text = "??"
    } else {
        val time = date.time
        val calendar = Calendar.getInstance().apply { timeInMillis = time }
        tv.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
    }
}

@BindingAdapter("monthFromDate")
fun monthFromDate(tv: TextView, date: Date?) {
    if (date == null) {
        tv.text = "?? ????"
    } else {
        tv.text = date.time.formatMonthYear().capitalize()
    }
}

@BindingAdapter("hourFromDate")
fun hourFromDate(tv: TextView, date: Date?) {
    if (date == null) {
        tv.text = "??:??"
    } else {
        tv.text = date.time.formatTimeWithoutSeconds()
    }
}
