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

package com.forcetower.uefs.feature.siecomp.bindings

import android.content.Context
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.uefs.R
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import org.threeten.bp.Duration
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

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
            durationString(textView.context, Duration.between(startTime, endTime)), room
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