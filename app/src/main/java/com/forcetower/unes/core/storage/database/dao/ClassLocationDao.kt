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
import com.forcetower.sagres.database.model.SDisciplineClassLocation
import com.forcetower.unes.core.model.unes.ClassGroup
import com.forcetower.unes.core.model.unes.ClassLocation
import com.forcetower.unes.core.model.unes.Profile
import com.forcetower.unes.core.model.unes.Semester
import com.forcetower.unes.core.storage.database.accessors.LocationWithGroup
import timber.log.Timber

@Dao
abstract class ClassLocationDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(location: ClassLocation)

    @Query("SELECT * FROM ClassLocation")
    abstract fun getAllLocationsDirect(): List<ClassLocation>

    @Transaction
    @Query("SELECT cl.* FROM ClassLocation cl, Profile p WHERE cl.profile_id = p.uid AND p.me = 1")
    abstract fun getCurrentSchedule(): LiveData<List<LocationWithGroup>>

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