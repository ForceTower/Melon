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
import com.forcetower.sagres.database.model.SDisciplineClassLocation
import com.forcetower.unes.core.model.*
import timber.log.Timber

@Dao
abstract class ClassLocationDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(location: ClassLocation)

    @Query("SELECT * FROM ClassLocation")
    abstract fun getAllLocationsDirect(): List<ClassLocation>

    @Transaction
    open fun putSchedule(locations: List<SDisciplineClassLocation>) {
        if (locations.isEmpty()) return

        val semester = selectCurrentSemesterDirect()
        val profile  = getMeProfile()
        wipeScheduleProfile(profile.uid)

        locations.forEach {
            val groups = selectGroups(semester.uid, it.classCode, profile.uid)
            if (groups.isNotEmpty()) {
                if (groups.size == 1) {
                    val group = groups[0]
                    prepareInsertion(group, profile, it)
                } else {
                    var group = groups[0]
                    groups.forEach { g ->
                        if (g.group == it.classGroup) {
                            group = g
                        }
                    }
                    prepareInsertion(group, profile, it)
                }
            } else {
                Timber.d("<location_404> :: Groups not found ${semester.codename}_${it.classCode}_${profile.name}")
            }
        }
    }

    private fun prepareInsertion(group: ClassGroup, profile: Profile, location: SDisciplineClassLocation) {
        val entity = ClassLocation(
                groupId = group.uid,
                profileId = profile.uid,
                startsAt = location.startTime,
                endsAt = location.endTime,
                campus = location.campus,
                room = location.room,
                day = location.day,
                modulo = location.modulo)

        insert(entity)
        group.group = location.classGroup
        update(group)
    }

    @Update
    protected abstract fun update(group: ClassGroup)

    @Query("SELECT g.* FROM ClassGroup g, ClassStudent cs, Class c, discipline d WHERE g.class_id = c.uid AND c.semester_id = :semesterUid AND c.discipline_id = d.uid AND d.code = :disciplineCode AND cs.group_id = g.uid AND cs.profile_id = :profileId")
    protected abstract fun selectGroups(semesterUid: Long, disciplineCode: String, profileId: Long): List<ClassGroup>

    @Query("SELECT * FROM Profile WHERE me = 1")
    protected abstract fun getMeProfile(): Profile

    @Query("SELECT * FROM Semester ORDER BY sagres_id DESC LIMIT 1")
    protected abstract fun selectCurrentSemesterDirect(): Semester

    //TODO Find a better way to wipe current locations
    @Query("DELETE FROM ClassLocation WHERE profile_id = :profileId")
    protected abstract fun wipeScheduleProfile(profileId: Long)
}