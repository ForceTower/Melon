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

package com.forcetower.uefs.core.task.definers

import androidx.room.withTransaction
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.UTask
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SemestersProcessor(
    private val database: UDatabase,
    private val semesters: List<dev.forcetower.breaker.model.Semester>
) : UTask {
    override suspend fun execute() {
        database.withTransaction {
            val data = semesters.map {
                val start = ZonedDateTime.parse(it.start, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val end = ZonedDateTime.parse(it.end, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                Semester(sagresId = it.id, name = it.code, codename = it.description, start = start.toEpochSecond() * 1000, end = end.toEpochSecond() * 1000)
            }
            database.semesterDao().insertIgnoring(data)
        }
    }
}
