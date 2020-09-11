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
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.uefs.R
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

@BindingAdapter(value = ["sessionStart", "sessionEnd", "sessionRoom", "timeZoneId"], requireAll = true)
fun sessionDurationLocation(
    textView: TextView,
    startTime: ZonedDateTime,
    endTime: ZonedDateTime,
    room: String,
    timeZoneId: ZoneId?
) {
    val finalTimeZoneId = timeZoneId ?: ZoneId.systemDefault()
    val localStartTime = TimeUtils.zonedTime(startTime, finalTimeZoneId)
    val localEndTime = TimeUtils.zonedTime(endTime, finalTimeZoneId)

    textView.text = textView.context.getString(
        R.string.event_session_duration_location,
        durationString(textView.context, Duration.between(startTime, endTime)),
        room
    )

    textView.contentDescription = fullDateTime(localStartTime, localEndTime, textView, room)
}

private fun durationString(context: Context, duration: Duration): String {
    val hours = duration.toHours()
    return if (hours > 0L) {
        context.resources.getQuantityString(R.plurals.duration_hours, hours.toInt(), hours)
    } else {
        val minutes = duration.toMinutes()
        context.resources.getQuantityString(R.plurals.duration_minutes, minutes.toInt(), minutes)
    }
}

private fun fullDateTime(
    localStartTime: ZonedDateTime,
    localEndTime: ZonedDateTime,
    textView: TextView,
    room: String
): String {
    val timeString = TimeUtils.timeString(localStartTime, localEndTime)
    return textView.context.getString(R.string.session_duration_location, timeString, room)
}
