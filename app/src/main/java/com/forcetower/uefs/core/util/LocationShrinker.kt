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

package com.forcetower.uefs.core.util

import com.forcetower.sagres.database.model.SagresDisciplineClassLocation
import com.forcetower.uefs.feature.shared.extensions.createTimeInt
import timber.log.Timber

object LocationShrinker {
    @JvmStatic
    fun shrink(locations: List<SagresDisciplineClassLocation>): List<SagresDisciplineClassLocation> {
        if (locations.isEmpty()) return emptyList()

        val grouped = locations.groupBy { it.day.trim() }
        val timers = locations
            .map { Timed(it.startTime.createTimeInt(), it.endTime.createTimeInt()) }
            .distinct()
            .sortedBy { it.start }

        val mapped = grouped.mapValues { entry ->
            val dayList = timers.map { timed ->
                val location = entry.value.find { it.startTime.createTimeInt() == timed.start && it.endTime.createTimeInt() == timed.end }
                val element = Element(timed, location)
                element
            }
            dayList
        }

        val shrunken = shrinker(mapped, timers)
        Timber.d("Shrunken value is: ${shrunken["SEG"]}")
        return shrunken.flatMap { it.value }.mapNotNull { it.reference }
    }

    /**
     * This method expects the List at every entry to be sorted and that every Element has a matching timed start/end
     */
    @JvmStatic
    private fun shrinker(map: Map<String, List<Element>>, timers: List<Timed>): Map<String, List<Element>> {
        var currentTime = timers[0]
        var index = 1

        val days = map.values
        val result = mutableMapOf<String, MutableList<Element>>()

        while (index < timers.size) {
            val nextTime = timers[index]

            currentTime = if (canJoin(currentTime, nextTime) && canJoinDayElements(days, index)) {
                Timber.d("Join day elements at index $index")
                currentTime.copy(end = nextTime.end)
            } else {
                Timber.d("Can't join $currentTime and $nextTime")
                savePendingToMap(result, currentTime, map)
                nextTime
            }

            index++
        }
        savePendingToMap(result, currentTime, map)
        return result
    }

    @JvmStatic
    private fun savePendingToMap(result: MutableMap<String, MutableList<Element>>, currentTime: Timed, original: Map<String, List<Element>>) {
        original.entries.forEach { entry ->
            val classesOnDayResult = result.getOrPut(entry.key) { mutableListOf() }
            val originalClassesOnPeriod = entry.value.filter {
                it.timed.start >= currentTime.start && it.timed.end <= currentTime.end
            }
            when {
                originalClassesOnPeriod.size > 1 -> {
                    val mutated = Element(
                        currentTime,
                        originalClassesOnPeriod[0].reference?.copy(
                            endTime = originalClassesOnPeriod.last().reference!!.endTime
                        )
                    )
                    classesOnDayResult.add(mutated)
                }
                originalClassesOnPeriod.size == 1 -> {
                    classesOnDayResult.add(originalClassesOnPeriod[0])
                }
                else -> {
                    Timber.d("Something smells fishy at $currentTime")
                }
            }
        }
    }

    @JvmStatic
    private fun canJoinDayElements(days: Collection<List<Element>>, index: Int): Boolean {
        return days.all {
            val current = it[index - 1]
            val next = it[index]
            canJoinElements(current, next)
        }.also {
            Timber.d("The result for joining days at index $index is $it")
        }
    }

    @JvmStatic
    private fun canJoin(currentTime: Timed, nextTime: Timed): Boolean {
        return currentTime.end == nextTime.start
    }

    @JvmStatic
    private fun canJoinElements(currentElement: Element, nextElement: Element): Boolean {
        return canJoin(currentElement.timed, nextElement.timed) &&
            currentElement.reference?.classCode == nextElement.reference?.classCode &&
            currentElement.reference?.classGroup == nextElement.reference?.classGroup
    }

    private data class Timed(
        val start: Int,
        val end: Int
    )

    private data class Element(
        val timed: Timed,
        val reference: SagresDisciplineClassLocation?
    )
}
