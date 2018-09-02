/*
 * Copyright (c) 2018.
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

package com.forcetower.unes.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import com.forcetower.sagres.database.model.SDiscipline
import com.forcetower.unes.core.model.unes.Class
import com.forcetower.unes.core.model.unes.Discipline
import com.forcetower.unes.core.model.unes.Semester

@Dao
abstract class ClassDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(classes: List<Class>)

    @Insert(onConflict = IGNORE)
    abstract fun insert(clazz: Class)

    @Update
    abstract fun update(clazz: Class)

    @Query("SELECT c.* FROM Class c, Semester s, Discipline d WHERE " +
            "c.discipline_id = d.uid AND " +
            "c.semester_id = s.uid AND " +
            "s.codename = :semester AND " +
            "d.code = :code")
    abstract fun getClassDirect(semester: String, code: String): Class?

    @Query("SELECT c.* FROM Class c, Semester s, Discipline d WHERE " +
            "c.discipline_id = d.uid AND " +
            "c.semester_id = s.uid AND " +
            "s.codename = :semester AND " +
            "d.code = :code")
    abstract fun getClass(semester: String, code: String): LiveData<Class?>

    @Query("SELECT c.* FROM Class c, Semester s WHERE c.semester_id = s.uid AND s.codename = :semester")
    abstract fun getClassesFromSemester(semester: String): LiveData<List<Class>>

    @Query("SELECT * FROM Discipline WHERE code = :code")
    protected abstract fun selectDisciplineDirect(code: String): Discipline

    @Query("SELECT * FROM Semester WHERE codename = :name")
    protected abstract fun selectSemesterDirect(name: String): Semester

    @Transaction
    open fun insert(dis: SDiscipline) {
        var clazz = getClassDirect(dis.semester, dis.code)
        if (clazz == null) {
            val discipline = selectDisciplineDirect(dis.code)
            val semester = selectSemesterDirect(dis.semester)
            clazz = Class(
                    disciplineId = discipline.uid,
                    semesterId = semester.uid
            )
            insert(clazz)
        }

        clazz.selectiveCopy(dis)
        update(clazz)
    }

    @Delete
    abstract fun delete(clazz: Class)

}