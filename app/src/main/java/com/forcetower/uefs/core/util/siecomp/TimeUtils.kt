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

package com.forcetower.uefs.core.util.siecomp

import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.core.model.siecomp.EventDay
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Provides default timers...
 * TODO This should be overridden by the cloud at any time!
 */
object TimeUtils {
    val SIECOMP_TIMEZONE = ZoneId.of(BuildConfig.SIECOMP_TIMEZONE)

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
}