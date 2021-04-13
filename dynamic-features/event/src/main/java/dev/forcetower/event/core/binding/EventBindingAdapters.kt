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

package dev.forcetower.event.core.binding

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import dev.forcetower.event.R
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@BindingAdapter("formattedDate")
fun formattedDate(tv: TextView, time: ZonedDateTime?) {
    time ?: return
    val date = DateTimeFormatter.ofPattern("d 'de' MMM ' ▪ ' H:mm").withLocale(Locale.getDefault()).format(time)
    tv.text = date
}

@BindingAdapter("eventPrice")
fun eventPrice(tv: TextView, price: Double?) {
    val context = tv.context
    val string = when (price) {
        null -> context.getString(R.string.event_free)
        else -> context.getString(R.string.event_price_format, price)
    }
    tv.text = string
}

@BindingAdapter("eventCertificate")
fun eventCertificate(tv: TextView, hours: Int?) {
    val context = tv.context
    if (hours == null) {
        tv.visibility = View.GONE
    } else {
        tv.visibility = View.VISIBLE
        val string = context.getString(R.string.event_certificate_hours_format, hours)
        tv.text = string
    }
}
