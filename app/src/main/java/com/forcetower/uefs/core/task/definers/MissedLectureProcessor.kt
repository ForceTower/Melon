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

import android.content.Context
import androidx.room.withTransaction
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.UTask
import dev.forcetower.breaker.model.LectureMissed

class MissedLectureProcessor(
    private val context: Context,
    private val database: UDatabase,
    // local database profile id
    private val profileId: Long,
    // local database group id :)
    private val groupId: Long,
    private val absences: List<LectureMissed>,
    private val notify: Boolean
) : UTask {
    override suspend fun execute() {
        database.withTransaction {
            val group = database.classGroupDao().getGroupDirect(groupId) ?: return@withTransaction
            val classId = group.classId

            val before = database.classAbsenceDao().getDirectWithDetails(classId)
            database.classAbsenceDao().resetClassAbsences(classId)

            val mapped = absences.map { absence ->
                ClassAbsence(
                    description = absence.lecture.subject ?: "Sem descrição",
                    date = absence.lecture.date ?: "Sem data",
                    notified = !notify,
                    profileId = profileId,
                    grouping = group.group,
                    classId = classId,
                    sequence = absence.lecture.ordinal
                )
            }

            database.classAbsenceDao().insert(mapped)

            val after = database.classAbsenceDao().getDirectWithDetails(classId)

            // before: [A, B, C, D]
            // after:  [B, C, D, E]

            // new:    [E]
            val new = after.filter { a -> before.any { !it.isSame(a) } }
            // removed [A]
            val removed = before.filter { b -> after.any { !it.isSame(b) } }

            new.forEach {
                // TODO Show notification
            }

            removed.forEach {
                // TODO Show notification
            }

            database.classAbsenceDao().markAllNotified()
        }
    }
}
