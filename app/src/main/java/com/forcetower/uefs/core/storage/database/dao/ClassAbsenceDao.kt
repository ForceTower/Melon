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

package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.sagres.database.model.SDisciplineMissedClass
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.model.unes.Profile
import timber.log.Timber

@Dao
abstract class ClassAbsenceDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(absence: ClassAbsence)

    @Query("UPDATE ClassAbsence SET notified = 1")
    abstract fun markAllNotified()

    @Query("SELECT * FROM ClassAbsence WHERE notified = 0")
    abstract fun getUnnotifiedDirect(): List<ClassAbsence>

    @Query("SELECT ca.* FROM ClassAbsence ca WHERE ca.class_id = :classId")
    abstract fun getMyAbsenceFromClass(classId: Long): LiveData<List<ClassAbsence>>

    @Query("SELECT ca.* FROM ClassAbsence ca WHERE ca.class_id = :classId")
    abstract fun getAbsenceFromClassDirect(classId: Long): List<ClassAbsence>

    @Transaction
    open fun putAbsences(classes: List<SDisciplineMissedClass>) {
        val profile = getMeProfile()

        classes.forEach {
            val sequence = it.description.split("-")[0].trim().split(" ")[1].trim().toIntOrNull() ?: 0
            val clazz = getClass(it.disciplineCode, it.semester)

            if (clazz != null) {
                insert(ClassAbsence(classId = clazz.uid, profileId = profile.uid, date = it.date, description = it.description, sequence = sequence, notified = false))
            } else {
                Timber.e("<abs_no_class> :: Class not found for ${it.disciplineCode}_${it.semester}")
            }
        }
    }

    @Query("SELECT c.* FROM Class c, Discipline d, Semester s WHERE c.semester_id = s.uid AND c.discipline_id = d.uid AND LOWER(d.code) = LOWER(:code) AND s.sagres_id = :semester")
    protected abstract fun getClass(code: String, semester: Long): Class?

    @Query("SELECT * FROM Profile WHERE me = 1")
    protected abstract fun getMeProfile(): Profile
}