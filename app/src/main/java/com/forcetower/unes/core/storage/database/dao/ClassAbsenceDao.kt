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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.sagres.database.model.SDisciplineMissedClass
import com.forcetower.unes.core.model.unes.ClassAbsence
import com.forcetower.unes.core.model.unes.ClassGroup
import com.forcetower.unes.core.model.unes.ClassStudent
import com.forcetower.unes.core.model.unes.Profile
import timber.log.Timber

@Dao
abstract class ClassAbsenceDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(absence: ClassAbsence)

    @Transaction
    open fun putAbsences(classes: List<SDisciplineMissedClass>) {
        val profile = getMeProfile()

        classes.forEach {
            val groups = getClassGroup(it.disciplineCode, it.semester, profile.uid)
            Timber.d("Groups: $groups")
            var sequence = 0
            try {
                sequence = it.description.split("-")[0].trim().split(" ")[1].trim().toInt()
            } catch (t: Throwable) { Timber.e(t) }

            if (groups.isEmpty()) Timber.e("<abs_no_groups> :: Groups not found for ${it.disciplineCode}_${it.semester}_${profile.name}")
            else {
                if (groups.size == 1) {
                    val group = groups[0]
                    val cs = getClassStudent(group.uid, profile.uid)
                    insert(ClassAbsence(classId = cs.uid, profileId = profile.uid, date = it.date, description = it.description, sequence = sequence))
                } else {
                    val value = groups.firstOrNull { g -> g.group.startsWith("T") }
                    if (value == null) {
                        Timber.e("<abs_no_T_found> :: This will be ignored forever ${it.disciplineCode}_${it.semester}_${profile.name} ")
                    } else {
                        val cs = getClassStudent(value.uid, profile.uid)
                        insert(ClassAbsence(classId = cs.uid, profileId = profile.uid, date = it.date, description = it.description, sequence = sequence))
                    }
                }
            }
        }
    }

    @Query("SELECT cs.* FROM ClassStudent cs WHERE cs.group_id = :groupId AND cs.profile_id = :profileId")
    protected abstract fun getClassStudent(groupId: Long, profileId: Long): ClassStudent

    @Query("SELECT cs.* FROM ClassStudent cs, ClassGroup g, Class c, Discipline d, Semester s " +
            "WHERE cs.profile_id = :profileId AND " +
            "cs.group_id = g.uid " +
            "AND g.class_id = c.uid " +
            "AND c.discipline_id = d.uid " +
            "AND d.code = :code " +
            "AND s.sagres_id = :semesterId " +
            "ORDER BY g.`group` ASC LIMIT 1")
    protected abstract fun getClassStudent(code: String, semesterId: Long, profileId: Long): ClassStudent

    @Query("SELECT g.* FROM ClassGroup g, Class c, Discipline d, Semester s, ClassStudent cs WHERE g.class_id = c.uid AND c.semester_id = s.uid AND c.discipline_id = d.uid AND s.sagres_id = :semesterId AND d.code = :code AND cs.profile_id = :profileId AND g.uid = cs.group_id")
    protected abstract fun getClassGroup(code: String, semesterId: Long, profileId: Long): List<ClassGroup>

    @Query("SELECT * FROM Profile WHERE me = 1")
    protected abstract fun getMeProfile(): Profile
}