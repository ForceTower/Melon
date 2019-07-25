/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.storage.repository

import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.database.accessors.ClassFullWithGroup
import com.forcetower.uefs.core.storage.database.accessors.ClassWithGroups
import com.forcetower.uefs.core.storage.database.accessors.GroupWithClass
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisciplinesRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors
) {
    fun getParticipatingSemesters(): LiveData<List<Semester>> {
        return database.semesterDao().getParticipatingSemesters()
    }

    fun getClassesWithGradesFromSemester(semesterId: Long): LiveData<List<ClassWithGroups>> {
        return database.classDao().getClassesWithGradesFromSemester(semesterId)
    }

    fun getClassGroup(classGroupId: Long): LiveData<GroupWithClass?> {
        return database.classGroupDao().getWithRelations(classGroupId)
    }

    fun getClassFull(classId: Long): LiveData<ClassFullWithGroup?> {
        return database.classDao().getClass(classId)
    }

    fun getMyAbsencesFromClass(classId: Long): LiveData<List<ClassAbsence>> {
        return database.classAbsenceDao().getMyAbsenceFromClass(classId)
    }

    fun getAbsencesAmount(classId: Long) = database.classAbsenceDao().getMissedClassesAmount(classId)

    fun getMaterialsFromGroup(classGroupId: Long): LiveData<List<ClassMaterial>> {
        return database.classMaterialDao().getMaterialsFromGroup(classGroupId)
    }

    fun getClassItemsFromGroup(classGroupId: Long): LiveData<List<ClassItem>> {
        return database.classItemDao().getClassItemsFromGroup(classGroupId)
    }

    @AnyThread
    fun loadClassDetails(groupId: Long): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        result.postValue(true)
        executors.networkIO().execute {
            Timber.d("Group id for load is $groupId")
            val value = database.classGroupDao().getWithRelationsDirect(groupId)
            if (value == null) {
                Timber.d("Class Group with ID: $groupId was not found")
                result.postValue(false)
            } else {
                val clazz = value.clazz()
                val semester = clazz.semester().name
                val code = clazz.discipline().code
                val group = value.group.group

                Timber.d("Code: $code. Semester: $semester. Group: $group")

                val callback = SagresNavigator.instance.disciplinesExperimental(semester, code, group)
                if (callback.status == Status.COMPLETED) {
                    val groups = callback.getGroups()
                    database.classGroupDao().defineGroups(groups)
                } else {
                    Timber.d("Load group has failed along the way")
                }
                result.postValue(false)
            }
        }
        return result
    }

    @AnyThread
    fun resetGroups(clazz: Class) {
        executors.diskIO().execute {
            val uid = clazz.uid
            val groups = database.classGroupDao().getGroupsFromClassDirect(uid)
            groups.forEach {
                val id = it.uid
                database.classItemDao().clearFromGroup(id)
                database.classMaterialDao().clearFromGroup(id)
            }
        }
    }
}