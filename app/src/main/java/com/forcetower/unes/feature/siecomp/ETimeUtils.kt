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

package com.forcetower.unes.feature.siecomp

import com.forcetower.unes.BuildConfig
import com.forcetower.unes.core.model.event.EventDay
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

object ETimeUtils {
    val SIECOMP_TIMEZONE: ZoneId = ZoneId.of(BuildConfig.SIECOMP_TIMEZONE)
    val SIECOMPDays = listOf(
            EventDay(
                    0,
                    ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY1_START),
                    ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY1_END)
            ),
            EventDay(
                    1,
                    ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY2_START),
                    ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY2_END)
            ),
            EventDay(
                    2,
                    ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY3_START),
                    ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY3_END)
            ),
            EventDay(
                    3,
                    ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY4_START),
                    ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY4_END)
            )
    )

    fun zonedTime(time: ZonedDateTime, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
        return ZonedDateTime.ofInstant(time.toInstant(), zoneId)
    }

    fun timeString(startTime: ZonedDateTime, endTime: ZonedDateTime): String {
        val sb = StringBuilder()
        sb.append(DateTimeFormatter.ofPattern("EEE, MMM d, H:mm ").format(startTime))

        val startTimeMeridiem: String = DateTimeFormatter.ofPattern("a").format(startTime)
        val endTimeMeridiem: String = DateTimeFormatter.ofPattern("a").format(endTime)
        if (startTimeMeridiem != endTimeMeridiem) {
            sb.append(startTimeMeridiem).append(" ")
        }

        sb.append(DateTimeFormatter.ofPattern("- H:mm a").format(endTime))
        return sb.toString()
    }
}