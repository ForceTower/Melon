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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.sagres.database.model.SDisciplineMissedClass
import com.forcetower.unes.core.model.ClassAbsence
import com.forcetower.unes.core.model.ClassGroup
import com.forcetower.unes.core.model.ClassStudent
import com.forcetower.unes.core.model.Profile
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