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

package dev.forcetower.conference.core.utils

import dev.forcetower.conference.core.model.persistence.ConferenceDay
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    private const val formatPattern = "d 'de' MMM"

    private fun getFormatter(pattern: String): DateTimeFormatter {
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    }

    fun getShortNameDay(day: ConferenceDay): String = getFormatter(formatPattern).format(day.start)
    fun getLongNameDay(day: ConferenceDay): String = getFormatter("EEEE, dd 'de' MMMM").format(day.start)
}
