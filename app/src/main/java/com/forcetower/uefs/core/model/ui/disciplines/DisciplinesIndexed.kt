/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.model.ui.disciplines

import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup

class DisciplinesIndexed(
    mapping: Map<Semester, Int>
) {
    // ensure map is ordered
    init {
        var previous = -1
        mapping.forEach { (_, position) ->
            if (position <= previous) {
                throw IllegalArgumentException("Index values must be >= 0 and in ascending order")
            }
            previous = position
        }
    }

    val semesters = mapping.map { it.key }
    private val startPositions = mapping.map { it.value }

    fun semesterForPosition(position: Int): Semester? {
        startPositions.asReversed().forEachIndexed { index, intVal ->
            if (intVal <= position) {
                return semesters[semesters.size - index - 1]
            }
        }
        return null
    }

    fun positionForSemester(semester: Semester): Int {
        val index = semesters.indexOf(semester)
        if (index == -1) {
            throw IllegalArgumentException("Unknown semester")
        }
        return startPositions[index]
    }

    companion object {
        fun from(groups: List<ClassFullWithGroup>): DisciplinesIndexed {
            // TODO Apply correct sorting option
            val semesters = groups.map { it.semester }.distinct().sortedBy { it.start }
            val mapping = semesters.associateWith { semester ->
                groups.indexOfFirst { it.semester.uid == semester.uid }
            }
            return DisciplinesIndexed(mapping)
        }
    }
}