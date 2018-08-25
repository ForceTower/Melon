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
import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.unes.core.model.Class
import com.forcetower.unes.core.model.ClassGroup
import com.forcetower.unes.core.model.Discipline
import timber.log.Timber

@Dao
abstract class ClassGroupDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(group: ClassGroup): Long

    @Transaction
    open fun insert(grp: SDisciplineGroup): ClassGroup {
        val sgr = if (grp.group == null) "unique" else grp.group
        val discipline = selectDisciplineDirect(grp.code)
        var group = selectGroupDirect(grp.semester, grp.code, sgr)
        if (group == null) {
            val clazz = selectClassDirect(grp.semester, grp.code)
            group = ClassGroup(classId = clazz.uid, group = sgr)
            group.uid = insert(group)
        }

        group.selectiveCopy(grp)
        update(group)

        if (!grp.department.isNullOrBlank()) {
            discipline.department = grp.department
            updateDiscipline(discipline)
            Timber.d("Updated discipline ${discipline.code} department to ${discipline.department}")
        }
        return group
    }

    @Update
    abstract fun update(group: ClassGroup)

    @Query("SELECT g.* FROM ClassGroup g, Class c, Semester s, Discipline d WHERE g.class_id = c.uid AND c.discipline_id = d.uid AND c.semester_id = s.uid AND s.codename = :semester AND d.code = :code AND g.`group` = :group")
    protected abstract fun selectGroupDirect(semester: String, code: String, group: String): ClassGroup?

    @Query("SELECT g.* FROM ClassGroup g, Class c, Semester s, Discipline d WHERE g.class_id = c.uid AND c.discipline_id = d.uid AND c.semester_id = s.uid AND s.codename = :semester AND d.code = :code AND g.`group` = :group")
    abstract fun selectGroup(semester: String, code: String, group: String): LiveData<ClassGroup?>

    @Query("SELECT c.* FROM Class c, Semester s, Discipline d WHERE " +
            "c.discipline_id = d.uid AND " +
            "c.semester_id = s.uid AND " +
            "s.codename = :semester AND " +
            "d.code = :code")
    protected abstract fun selectClassDirect(semester: String, code: String): Class

    @Query("SELECT * FROM Discipline WHERE code = :code")
    protected abstract fun selectDisciplineDirect(code: String): Discipline

    @Update
    protected abstract fun updateDiscipline(discipline: Discipline)
}