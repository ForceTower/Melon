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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.forcetower.sagres.database.model.SagresDiscipline
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup
import com.forcetower.uefs.core.storage.database.aggregation.ClassWithDiscipline
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

@Dao
abstract class ClassDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(classes: List<Class>)

    @Insert(onConflict = IGNORE)
    abstract fun insert(clazz: Class): Long

    @Update
    abstract fun update(clazz: Class)

    @Query(
        "SELECT c.* FROM Class c, Semester s, Discipline d WHERE " +
            "c.discipline_id = d.uid AND " +
            "c.semester_id = s.uid AND " +
            "s.codename = :semester AND " +
            "LOWER(d.code) = LOWER(:code)"
    )
    abstract fun getClassDirect(semester: String, code: String): Class?

    @Query(
        "SELECT c.* FROM Class c, Semester s, Discipline d WHERE " +
            "c.discipline_id = d.uid AND " +
            "c.semester_id = s.uid AND " +
            "s.codename = :semester AND " +
            "LOWER(d.code) = LOWER(:code)"
    )
    abstract fun getClass(semester: String, code: String): LiveData<Class?>

    @Transaction
    @Query("SELECT * FROM Class c WHERE c.uid = :classId")
    abstract fun getClass(classId: Long): LiveData<ClassFullWithGroup?>

    @Query("SELECT c.* FROM Class c, Semester s WHERE c.semester_id = s.uid AND s.codename = :semester AND c.schedule_only = 0")
    abstract fun getClassesFromSemester(semester: String): LiveData<List<Class>>

    @Transaction
    @Query("SELECT c.* FROM Class c WHERE c.semester_id = :semesterId AND c.schedule_only = 0")
    abstract fun getClassesWithGradesFromSemester(semesterId: Long): LiveData<List<ClassFullWithGroup>>

    @Transaction
    @Query("SELECT c.* FROM Class c WHERE c.schedule_only = 0")
    abstract fun getClassesWithGradesFromAllSemesters(): Flow<List<ClassFullWithGroup>>

    @Transaction
    @Query("SELECT c.* FROM Class c WHERE c.semester_id = :semesterId AND c.schedule_only = 0")
    abstract fun getClassesWithGradesFromSemesterDirect(semesterId: Long): List<ClassFullWithGroup>

    @Query("SELECT * FROM Discipline WHERE LOWER(code) = LOWER(:code)")
    protected abstract fun selectDisciplineDirect(code: String): Discipline?

    @Query("SELECT * FROM Semester WHERE LOWER(codename) = LOWER(:name)")
    protected abstract fun selectSemesterDirect(name: String): Semester?

    @Transaction
    open fun insert(dis: SagresDiscipline, validated: Boolean) {
        Timber.d("Unformatted discipline $dis")
        var clazz = getClassDirect(dis.semester.trim(), dis.code.trim())
        Timber.d("Inserting clazz... $clazz")
        if (clazz == null) {
            val discipline = selectDisciplineDirect(dis.code.trim())
            val semester = selectSemesterDirect(dis.semester.trim())
            Timber.d("looking for discipline $discipline at $semester")
            if (semester != null && discipline != null) {
                clazz = Class(
                    disciplineId = discipline.uid,
                    semesterId = semester.uid
                )
                insert(clazz)
            } else {
                if (semester == null) {
                    Timber.e("Semester not found ${dis.semester.trim()}")
                }
                if (discipline != null) {
                    Timber.e("Discipline not found ${discipline.code}")
                }
            }
        }

        if (clazz != null) {
            clazz.selectiveCopy(dis, validated)
            update(clazz)
        }
    }

    open suspend fun insertNewWays(clazz: Class): Long {
        val current = getClassDirectlyNew(clazz.semesterId, clazz.disciplineId)
        return if (current != null) {
            update(current.copy(finalScore = clazz.finalScore, missedClasses = clazz.missedClasses))
            current.uid
        } else {
            insert(clazz)
        }
    }

    @Query("SELECT * FROM Class WHERE semester_id = :semesterId AND discipline_id = :disciplineId")
    abstract suspend fun getClassDirectlyNew(semesterId: Long, disciplineId: Long): Class?

    @Delete
    abstract fun delete(clazz: Class)

    @Query("DELETE FROM Class")
    abstract fun deleteAll()

    @Transaction
    @Query("SELECT * FROM Class")
    abstract fun getAll(): LiveData<List<ClassWithDiscipline>>

    @Transaction
    @Query("SELECT * FROM Class")
    abstract fun getAllDirect(): List<ClassWithDiscipline>
}
