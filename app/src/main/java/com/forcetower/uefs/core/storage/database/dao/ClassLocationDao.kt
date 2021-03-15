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

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.forcetower.sagres.database.model.SagresDisciplineClassLocation
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.ClassLocation
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.aggregation.ClassLocationWithData
import com.forcetower.uefs.feature.shared.extensions.createTimeInt
import com.forcetower.uefs.feature.shared.extensions.fromWeekDay
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

@Dao
abstract class ClassLocationDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(location: ClassLocation)

    @Query("SELECT * FROM ClassLocation")
    abstract fun getAllLocationsDirect(): List<ClassLocation>

    @Transaction
    @Query("SELECT cl.* FROM ClassLocation cl, Profile p WHERE cl.profile_id = p.uid AND p.me = 1 AND cl.hidden_on_schedule = 0")
    abstract fun getCurrentVisibleSchedule(): LiveData<List<ClassLocationWithData>>

    @Transaction
    @Query("SELECT cl.* FROM ClassLocation cl, Profile p WHERE cl.profile_id = p.uid AND p.me = 1 AND cl.hidden_on_schedule = 0")
    abstract fun getCurrentVisibleSchedulePerformance(): Flow<List<ClassLocationWithData>>

    @Transaction
    @Query("SELECT cl.* FROM ClassLocation cl, Profile p WHERE cl.profile_id = p.uid AND p.me = 1 AND cl.hidden_on_schedule = 0 AND cl.dayInt = :dayInt AND (((cl.endsAtInt - cl.startsAtInt) / 2) + cl.startsAtInt) > :currentTimeInt ORDER BY startsAtInt LIMIT 1")
    abstract fun getCurrentClass(dayInt: Int, currentTimeInt: Int): LiveData<ClassLocationWithData?>

    @Transaction
    @Query("SELECT cl.* FROM ClassLocation cl, Profile p WHERE cl.profile_id = p.uid AND p.me = 1 AND cl.hidden_on_schedule = 0 AND cl.dayInt = :dayInt AND cl.startsAtInt >= :currentTimeInt ORDER BY startsAtInt LIMIT 1")
    abstract suspend fun getCurrentClassDirect(dayInt: Int, currentTimeInt: Int): ClassLocationWithData?

    @Query("SELECT cl.* FROM ClassLocation cl")
    abstract fun getCurrentScheduleDirect(): List<ClassLocation>

    @Query("SELECT cl.* FROM ClassLocation cl INNER JOIN ClassGroup cg ON cl.group_id = cg.uid WHERE cg.class_id = :classId GROUP BY cl.uid")
    abstract fun getLocationsOfClass(classId: Long): LiveData<List<ClassLocation>>

    @WorkerThread
    @Query("UPDATE ClassLocation SET hidden_on_schedule = :hide WHERE group_id = :groupId AND day = :day AND starts_at = :startsAt AND ends_at = :endsAt AND profile_id = :profileId")
    abstract fun setClassHiddenHidden(hide: Boolean, groupId: Long, day: String, startsAt: String, endsAt: String, profileId: Long): Int

    @WorkerThread
    @Query("UPDATE ClassLocation SET hidden_on_schedule = :hide WHERE uid = :locationId")
    abstract fun setClassHiddenHidden(hide: Boolean, locationId: Long): Int

    @Query("SELECT COUNT(uid) FROM ClassLocation WHERE hidden_on_schedule = 1")
    abstract fun getHiddenClassesCount(): LiveData<Int>

    @Transaction
    open suspend fun putNewSchedule(allocations: List<ClassLocation>) {
        if (allocations.isEmpty()) return

        val profile = getMeProfile()
        profile ?: return

        val hidden = getHiddenLocations()
        wipeScheduleProfile(profile.uid)

        allocations.forEach { insert(it) }
        hidden.forEach { setClassHiddenHidden(true, it.groupId, it.day, it.startsAt, it.endsAt, it.profileId) }
    }

    @WorkerThread
    @Transaction
    open fun putSchedule(locations: List<SagresDisciplineClassLocation>, deterministic: Boolean) {
        if (locations.isEmpty()) return

        val semester = if (deterministic) {
            selectCurrentSemesterDirect()
        } else {
            selectAllSemestersDirect().minOrNull()
        }

        val profile = getMeProfile()
        if (semester == null || profile == null) return
        val hidden = getHiddenLocations()
        wipeScheduleProfile(profile.uid)

        locations.forEach {
            val groups = selectGroups(semester.uid, it.classCode)
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

                var discipline = selectDisciplineDirect(it.classCode.trim())

                val other = selectDisciplineByName(it.className)
                Timber.d("other is other code: ${other?.code} current code ${it.classCode} $other")
                if (discipline == null) {
                    Timber.d("Creating a new discipline")
                    val fakeDiscipline = Discipline(name = it.className.trim(), code = it.classCode.trim(), credits = 0)
                    val id = insertDiscipline(fakeDiscipline)
                    fakeDiscipline.uid = id
                    discipline = fakeDiscipline
                }

                Timber.d("After effects Discipline $discipline")

                if (discipline.uid > 0) {
                    var clazz = selectClass(discipline.uid, semester.uid)
                    if (clazz == null) {
                        Timber.d("Creating a new class")
                        val newClazz = Class(disciplineId = discipline.uid, semesterId = semester.uid, scheduleOnly = true)
                        val id = insertClass(newClazz)
                        clazz = newClazz
                        clazz.uid = id
                    }

                    Timber.d("After effects clazz $clazz")

                    val id = clazz.uid
                    if (id > 0) {
                        var group = selectGroup(id, it.classGroup.trim())
                        Timber.d("class_id is $id and group is ${it.classGroup}")
                        if (group == null) {
                            group = ClassGroup(classId = id, group = it.classGroup.trim())
                            val groupId = insertGroup(group!!)
                            group!!.uid = groupId
                        }
                        if (group!!.uid > 0) {
                            prepareInsertion(group!!, profile, it)
                        } else {
                            Timber.e(Exception("Avoided exception:: Class Group -${it.classGroup}- Discipline Code: -${it.classCode}- Name: -${it.className}-"))
                        }
                    } else {
                        Timber.e(Exception("Avoided exception:: disc_id -${discipline.uid}- smt_id: -${semester.uid}- Discipline Code: -${it.classCode}- Name: -${it.className}- Class Group -${it.classGroup}-"))
                    }
                } else {
                    Timber.e(Exception("Avoided exception:: Discipline Code: -${it.classCode}- Name: -${it.className}- Class Group -${it.classGroup}-"))
                }
            }
        }

        hidden.forEach { setClassHiddenHidden(true, it.groupId, it.day, it.startsAt, it.endsAt, it.profileId) }
    }

    @WorkerThread
    @Query("SELECT * FROM ClassLocation WHERE hidden_on_schedule = 1")
    protected abstract fun getHiddenLocations(): List<ClassLocation>

    @Query("SELECT * FROM Discipline WHERE name = :className")
    protected abstract fun selectDisciplineByName(className: String): Discipline?

    @Query("SELECT * FROM Class WHERE discipline_id = :disciplineId AND semester_id = :semesterId")
    protected abstract fun selectClass(disciplineId: Long, semesterId: Long): Class?

    @Query("SELECT * FROM ClassGroup WHERE class_id = :classId AND `group` = :group")
    protected abstract fun selectGroup(classId: Long, group: String): ClassGroup?

    private fun prepareInsertion(group: ClassGroup, profile: Profile, location: SagresDisciplineClassLocation) {
        val entity = ClassLocation(
            groupId = group.uid,
            profileId = profile.uid,
            startsAt = location.startTime,
            endsAt = location.endTime,
            campus = location.campus,
            room = location.room,
            day = location.day,
            modulo = location.modulo,
            startsAtInt = location.startTime.createTimeInt(),
            endsAtInt = location.endTime.createTimeInt(),
            dayInt = location.day.fromWeekDay()
        )

        insert(entity)
        group.group = location.classGroup
        update(group)
    }

    @Update(onConflict = REPLACE)
    protected abstract fun update(group: ClassGroup)

    @Query("SELECT g.* FROM ClassGroup g, Class c, discipline d WHERE g.class_id = c.uid AND c.semester_id = :semesterUid AND c.discipline_id = d.uid AND LOWER(d.code) = LOWER(:disciplineCode)")
    protected abstract fun selectGroups(semesterUid: Long, disciplineCode: String): List<ClassGroup>

    // There's almost 0 chance for this to happen, but, if user log's out during update this will
    // be called concurrently
    @Query("SELECT * FROM Profile WHERE me = 1")
    protected abstract fun getMeProfile(): Profile?

    // There's a really rare bug (1 occurrence) where the user got no semesters defined
    @Query("SELECT * FROM Semester ORDER BY sagres_id DESC LIMIT 1")
    protected abstract fun selectCurrentSemesterDirect(): Semester?

    @Query("SELECT * FROM Semester")
    protected abstract fun selectAllSemestersDirect(): List<Semester>

    // TODO Find a better way to wipe current locations
    @Query("DELETE FROM ClassLocation WHERE profile_id = :profileId")
    protected abstract fun wipeScheduleProfile(profileId: Long)

    @Query("SELECT * FROM Discipline WHERE LOWER(code) = LOWER(:code)")
    protected abstract fun selectDisciplineDirect(code: String): Discipline?

    @Insert(onConflict = IGNORE)
    protected abstract fun insertDiscipline(discipline: Discipline): Long

    @Insert(onConflict = IGNORE)
    protected abstract fun insertClass(clazz: Class): Long

    @Insert(onConflict = IGNORE)
    protected abstract fun insertGroup(group: ClassGroup): Long

    @Query("SELECT COUNT(cl.uid) FROM ClassLocation cl, Profile p WHERE cl.profile_id = p.uid AND p.me = 1 AND cl.hidden_on_schedule = 0")
    abstract fun hasSchedule(): LiveData<Boolean>
}
