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

package com.forcetower.uefs.core.storage.repository

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

    fun getMaterialsFromGroup(classGroupId: Long): LiveData<List<ClassMaterial>> {
        return database.classMaterialDao().getMaterialsFromGroup(classGroupId)
    }

    fun getClassItemsFromGroup(classGroupId: Long): LiveData<List<ClassItem>> {
        return database.classItemDao().getClassItemsFromGroup(classGroupId)
    }

    fun loadClassDetails(semester: String, code: String, group: String) {
        executors.networkIO().execute {
            SagresNavigator.instance.loadDisciplineDetails(semester, code, group)
        }
    }

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

                val callback = SagresNavigator.instance.loadDisciplineDetails(semester, code, group)
                if (callback.status == Status.COMPLETED) {
                    val groups = callback.getGroups()
                    if (groups != null) {
                        database.classGroupDao().defineGroups(groups)
                    } else {
                        Timber.d("It says it's completed but groups were null...")
                    }
                } else {
                    Timber.d("Load group has failed along the way")
                }
                result.postValue(false)
            }
        }
        return result
    }

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