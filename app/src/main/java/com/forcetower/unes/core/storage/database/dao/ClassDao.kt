/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import com.forcetower.sagres.database.model.SDiscipline
import com.forcetower.unes.core.model.Class
import com.forcetower.unes.core.model.Discipline
import com.forcetower.unes.core.model.Semester

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