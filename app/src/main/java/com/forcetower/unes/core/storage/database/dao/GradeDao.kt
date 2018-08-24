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

import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import com.forcetower.sagres.database.model.SGrade
import com.forcetower.sagres.database.model.SGradeInfo
import com.forcetower.unes.core.model.ClassGroup
import com.forcetower.unes.core.model.ClassStudent
import com.forcetower.unes.core.model.Grade
import com.forcetower.unes.core.model.Profile
import timber.log.Timber

@Dao
abstract class GradeDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(grade: Grade): Long

    @Update(onConflict = IGNORE)
    abstract fun update(grade: Grade)

    @Query("SELECT * FROM Grade")
    abstract fun getAllGradesDirect(): List<Grade>

    @Transaction
    open fun putGrades(grades: List<SGrade>) {
        val profile = getMeProfile()

        grades.forEach {
            val code = it.discipline.split("-")[0].trim()
            val groups = getClassGroup(code, it.semesterId, profile.uid)
            if (groups.isEmpty()) Timber.d("<grades_group_404> :: Groups not found for ${code}_${it.semesterId}_${profile.name}")
            else {
                if (groups.size == 1) {
                    val group = groups[0]
                    val cs = getClassStudent(group.uid, profile.uid)
                    prepareInsertion(cs, it)
                } else {
                    val value = groups.firstOrNull { g -> g.group.startsWith("T") }
                    if (value == null) {
                        Timber.e("<grades_no_T_found> :: This will be ignored forever ${code}_${it.semesterId}_${profile.name} ")
                    } else {
                        val cs = getClassStudent(value.uid, profile.uid)
                        prepareInsertion(cs, it)
                    }
                }
            }
        }
    }

    private fun prepareInsertion(cs: ClassStudent, it: SGrade) {
        val values = HashMap<String, SGradeInfo>()

        it.values.forEach {g ->
            var grade = values[g.name]
            if (grade == null) { grade = g }
            else {
                if (g.hasGrade()) grade = g
                else if (g.hasDate() && grade.hasDate() && g.date != grade.date) grade = g
                else Timber.d("This grade was ignored ${g.name}_${g.grade}")
            }

            values[g.name] = grade
        }

        values.values.forEach{ i ->
            val grade = getNamedGradeDirect(cs.uid, i.name)
            if (grade == null) {
                val notified = if (i.hasGrade()) 3 else 1
                insert(Grade(classId = cs.uid, name = i.name, date = i.date, notified = notified, grade = i.grade))
            } else {
                if (grade.hasGrade() && i.hasGrade() && grade.grade != i.grade) {
                    grade.notified = 4
                    grade.grade = i.grade
                    grade.date = i.date
                } else if (!grade.hasGrade() && i.hasGrade()) {
                    grade.notified = 3
                    grade.grade = i.grade
                    grade.date = i.date
                } else if (!grade.hasGrade() && !i.hasGrade() && grade.date != i.date) {
                    grade.notified = 2
                    grade.date = i.date
                } else {
                    Timber.d("No changes detected between $grade and $i")
                }
                update(grade)
            }
        }
    }

    @Query("SELECT * FROM Grade WHERE class_id = :classId AND name = :name")
    protected abstract fun getNamedGradeDirect(classId: Long, name: String): Grade?

    @Query("SELECT cs.* FROM ClassStudent cs WHERE cs.group_id = :groupId AND cs.profile_id = :profileId")
    protected abstract fun getClassStudent(groupId: Long, profileId: Long): ClassStudent

    @Query("SELECT g.* FROM ClassGroup g, Class c, Discipline d, Semester s, ClassStudent cs WHERE g.class_id = c.uid AND c.semester_id = s.uid AND c.discipline_id = d.uid AND s.sagres_id = :semesterId AND d.code = :code AND cs.profile_id = :profileId AND g.uid = cs.group_id")
    protected abstract fun getClassGroup(code: String, semesterId: Long, profileId: Long): List<ClassGroup>

    @Query("SELECT * FROM Profile WHERE me = 1")
    protected abstract fun getMeProfile(): Profile
}