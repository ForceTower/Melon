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

package com.forcetower.uefs.feature.messages

import android.text.SpannableString
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
fun getTimeStampedDate(view: TextView, message: Message) {
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
        tv.text = title ?: "::prov_renatinha::"
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