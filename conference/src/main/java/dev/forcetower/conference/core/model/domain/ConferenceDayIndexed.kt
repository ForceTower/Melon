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

package dev.forcetower.conference.core.model.domain

import dev.forcetower.conference.core.model.persistence.ConferenceDay

/**
 * Class maps the conference days / index of first session of that day on the list.
 * This will be used for scrolling to position in the future
 */
class ConferenceDayIndexed(
    mapping: Map<ConferenceDay, Int>
) {
    // Ensure map is ordered
    init {
        var previous = -1
        mapping.forEach { (_, position) ->
            if (position <= previous) {
                throw IllegalArgumentException("Index values must be >= 0 and in ascending order")
            }
            previous = position
        }
    }

    val days = mapping.map { it.key }
    private val startPositions = mapping.map { it.value }

    fun dayForPosition(position: Int): ConferenceDay? {
        startPositions.asReversed().forEachIndexed { index, intVal ->
            if (intVal <= position) {
                return days[days.size - index - 1]
            }
        }
        return null
    }

    fun positionForDay(day: ConferenceDay): Int {
        val index = days.indexOf(day)
        if (index == -1) {
            throw IllegalArgumentException("Unknown day")
        }
        return startPositions[index]
    }
}
