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

package com.forcetower.uefs.core.util.siecomp

import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.core.model.siecomp.EventDay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Provides default timers...
 * TODO This should be overridden by the cloud at any time!
 */
object TimeUtils {
    val SIECOMP_TIMEZONE: ZoneId = ZoneId.of(BuildConfig.SIECOMP_TIMEZONE)

    val EventDays = listOf(
        EventDay(
            1,
            ZonedDateTime.parse(BuildConfig.SIECOMP_DAY1_START),
            ZonedDateTime.parse(BuildConfig.SIECOMP_DAY1_END)
        ),
        EventDay(
            2,
            ZonedDateTime.parse(BuildConfig.SIECOMP_DAY2_START),
            ZonedDateTime.parse(BuildConfig.SIECOMP_DAY2_END)
        ),
        EventDay(
            3,
            ZonedDateTime.parse(BuildConfig.SIECOMP_DAY3_START),
            ZonedDateTime.parse(BuildConfig.SIECOMP_DAY3_END)
        ),
        EventDay(
            4,
            ZonedDateTime.parse(BuildConfig.SIECOMP_DAY4_START),
            ZonedDateTime.parse(BuildConfig.SIECOMP_DAY4_END)
        ),
        EventDay(
            5,
            ZonedDateTime.parse(BuildConfig.SIECOMP_DAY5_START),
            ZonedDateTime.parse(BuildConfig.SIECOMP_DAY5_END)
        )
    )

    fun eventHasStarted(): Boolean {
        return ZonedDateTime.now().isAfter(EventDays.first().start)
    }

    fun eventHasEnded(): Boolean {
        return ZonedDateTime.now().isAfter(EventDays.last().end)
    }

    fun zonedTime(time: ZonedDateTime, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
        return ZonedDateTime.ofInstant(time.toInstant(), zoneId)
    }

    fun timeString(startTime: ZonedDateTime, endTime: ZonedDateTime): String {
        val sb = StringBuilder()
        sb.append(DateTimeFormatter.ofPattern("EEE, d 'de' MMM, H:mm").withLocale(Locale.getDefault()).format(startTime))
        sb.append(DateTimeFormatter.ofPattern(" - H:mm").format(endTime))
        return sb.toString()
    }
}
