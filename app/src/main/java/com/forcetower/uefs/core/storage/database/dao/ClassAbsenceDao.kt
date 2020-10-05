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

package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.forcetower.sagres.database.model.SagresDisciplineMissedClass
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.storage.database.aggregation.ClassAbsenceWithClass
import timber.log.Timber

@Dao
abstract class ClassAbsenceDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(absence: ClassAbsence)

    @Insert(onConflict = IGNORE)
    abstract suspend fun insert(absence: List<ClassAbsence>)

    @Update(onConflict = IGNORE)
    abstract suspend fun update(absence: ClassAbsence)

    @Query("UPDATE ClassAbsence SET notified = 1")
    abstract suspend fun markAllNotified()

    @Query("SELECT * FROM ClassAbsence WHERE notified = 0")
    abstract suspend fun getUnnotifiedDirect(): List<ClassAbsence>

    @Query("SELECT ca.* FROM ClassAbsence ca WHERE ca.class_id = :classId ORDER BY ca.sequence")
    abstract fun getMyAbsenceFromClass(classId: Long): LiveData<List<ClassAbsence>>

    @Query("SELECT COUNT(uid) FROM ClassAbsence WHERE class_id = :classId")
    abstract fun getMissedClassesAmount(classId: Long): LiveData<Int>

    @Query("SELECT ca.* FROM ClassAbsence ca WHERE ca.class_id = :classId")
    abstract fun getAbsenceFromClassDirect(classId: Long): List<ClassAbsence>

    @Transaction
    open fun putAbsences(classes: List<SagresDisciplineMissedClass>) {
        val profile = getMeProfile()

        classes.mapNotNull { getClass(it.disciplineCode, it.semester) }
            .distinctBy { it.uid }
            .forEach { resetClassAbsences(it.uid) }

        classes.forEach {
            try {
                val sequence = it.description.split("-")[0].trim().split(" ")[1].trim().toIntOrNull() ?: 0
                val clazz = getClass(it.disciplineCode, it.semester)

                if (clazz != null) {
                    insert(
                        ClassAbsence(
                            classId = clazz.uid,
                            profileId = profile.uid,
                            date = it.date,
                            description = it.description,
                            sequence = sequence,
                            notified = false,
                            grouping = it.group
                        )
                    )
                } else {
                    Timber.e("<abs_no_class> :: Class not found for ${it.disciplineCode}_${it.semester}")
                }
            } catch (exception: Throwable) {
                Timber.e(exception, "Something went wrong at sequence extraction")
            }
        }
    }

    @Query("SELECT c.* FROM Class c, Discipline d, Semester s WHERE c.semester_id = s.uid AND c.discipline_id = d.uid AND LOWER(d.code) = LOWER(:code) AND s.sagres_id = :semester")
    protected abstract fun getClass(code: String, semester: Long): Class?

    @Query("SELECT * FROM Profile WHERE me = 1")
    protected abstract fun getMeProfile(): Profile

    @Query("DELETE FROM ClassAbsence WHERE class_id = :classId")
    abstract fun resetClassAbsences(classId: Long)

    @Transaction
    @Query("SELECT * FROM ClassAbsence WHERE class_id = :classId")
    abstract fun getDirectWithDetails(classId: Long): List<ClassAbsenceWithClass>
}
