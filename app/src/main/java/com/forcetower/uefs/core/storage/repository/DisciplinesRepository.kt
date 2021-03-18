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

package com.forcetower.uefs.core.storage.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.sagres.Constants
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.ui.disciplines.DisciplineHelperData
import com.forcetower.uefs.core.model.ui.disciplines.DisciplinesDataUI
import com.forcetower.uefs.core.model.ui.disciplines.DisciplinesIndexed
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.core.model.unes.ClassLocation
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup
import com.forcetower.uefs.core.storage.database.aggregation.ClassGroupWithData
import com.forcetower.uefs.core.storage.database.aggregation.ClassLocationWithData
import com.forcetower.uefs.core.task.definers.LectureProcessor
import com.forcetower.uefs.core.task.definers.MissedLectureProcessor
import dagger.Reusable
import dev.forcetower.breaker.Orchestra
import dev.forcetower.breaker.model.Authorization
import dev.forcetower.breaker.result.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

@Reusable
class DisciplinesRepository @Inject constructor(
    private val context: Context,
    private val client: OkHttpClient,
    private val database: UDatabase,
    private val executors: AppExecutors,
    @Named("webViewUA") private val agent: String,
    @Named("flagSnowpiercerEnabled") private val snowpiercerEnabled: Boolean,
    private val preferences: SharedPreferences
) {
    fun getAllDisciplinesData(): Flow<DisciplinesDataUI> {
        return database.classDao().getClassesWithGradesFromAllSemesters().map { classes ->
            val databaseSemesters = database.semesterDao().getParticipatingSemestersDirect()
            val classesSemester = classes.map { it.semester }.distinct()
            val semesters = (databaseSemesters + classesSemester).distinct().run {
                if (snowpiercerEnabled && all { it.start != null }) {
                    sortedByDescending { it.start }
                } else if (preferences.getBoolean("stg_semester_deterministic_ordering", true)) {
                    sortedByDescending { it.sagresId }
                } else {
                    sorted()
                }
            }
            val elements = transformClassesIntoUiElements(semesters, classes)
            val indexes = DisciplinesIndexed.from(semesters, elements)
            DisciplinesDataUI(elements, indexes, semesters)
        }
    }

    private fun transformClassesIntoUiElements(
        semesters: List<Semester>,
        classes: List<ClassFullWithGroup>
    ): List<DisciplineHelperData> {
        val completedMap = classes
            .groupBy { it.semester }
            .mapValues { entry ->
                val disciplines = entry.value
                val result = mutableListOf<DisciplineHelperData>()
                disciplines.sortedBy { it.discipline.name }.forEachIndexed { index, clazz ->
                    if (index != 0)
                        result += DisciplineHelperData.Divider

                    result += DisciplineHelperData.Header(clazz)

                    val groupings = clazz.grades.groupBy { it.grouping }
                    if (groupings.keys.size <= 1) {
                        clazz.grades.sortedBy { it.name }.forEach { grade ->
                            result += DisciplineHelperData.Score(clazz, grade)
                        }
                    } else {
                        groupings.entries.sortedBy { it.key }.forEach { (_, value) ->
                            if (value.isNotEmpty()) {
                                val sample = value[0]
                                result += DisciplineHelperData.GroupingName(clazz, sample.groupingName)
                                value.sortedBy { it.name }.forEach { grade ->
                                    result += DisciplineHelperData.Score(clazz, grade)
                                }
                            }
                        }
                    }

                    if (clazz.clazz.isInFinal()) {
                        result += DisciplineHelperData.Final(clazz)
                    }
                    result += DisciplineHelperData.Mean(clazz)
                }
                result
            }

        return semesters.map { completedMap[it] ?: listOf(DisciplineHelperData.EmptySemester(it)) }.flatten()
    }

    fun getParticipatingSemesters(): LiveData<List<Semester>> {
        return database.semesterDao().getParticipatingSemesters()
    }

    fun getClassesWithGradesFromSemester(semesterId: Long): LiveData<List<ClassFullWithGroup>> {
        return database.classDao().getClassesWithGradesFromSemester(semesterId)
    }

    fun getClassGroup(classGroupId: Long): LiveData<ClassGroupWithData?> {
        return database.classGroupDao().getWithRelations(classGroupId)
    }

    fun getClassFull(classId: Long): LiveData<ClassFullWithGroup?> {
        return database.classDao().getClass(classId)
    }

    fun getMyAbsencesFromClass(classId: Long): LiveData<List<ClassAbsence>> {
        return database.classAbsenceDao().getMyAbsenceFromClass(classId)
    }

    fun getAbsencesAmount(classId: Long): LiveData<Int> {
        return database.classAbsenceDao().getMissedClassesAmount(classId)
    }

    fun getMaterialsFromGroup(classGroupId: Long): LiveData<List<ClassMaterial>> {
        return database.classMaterialDao().getMaterialsFromGroup(classGroupId)
    }

    fun getClassItemsFromGroup(classGroupId: Long): LiveData<List<ClassItem>> {
        return database.classItemDao().getClassItemsFromGroup(classGroupId)
    }

    fun getLocationsFromClass(classId: Long): LiveData<List<ClassLocation>> {
        return database.classLocationDao().getLocationsOfClass(classId)
    }

    suspend fun getCurrentClass(): ClassLocationWithData? {
        val calendar = Calendar.getInstance()
        val dayInt = calendar.get(Calendar.DAY_OF_WEEK)
        val currentTimeInt = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return database.classLocationDao().getCurrentClassDirect(dayInt, currentTimeInt)
    }

    fun loadClassDetailsSnowflake(groupId: Long) = flow {
        emit(true)
        val access = database.accessDao().getAccessDirect()
        val profile = database.profileDao().selectMeDirect()

        if (access == null || profile == null) {
            emit(false)
            return@flow
        }

        val orchestra = Orchestra.Builder()
            .userAgent(agent)
            .client(client)
            .build()

        orchestra.setAuthorization(Authorization(access.username, access.password))

        val sagresGroupId = database.classGroupDao().getGroupDirect(groupId)?.sagresId
        if (sagresGroupId != null) {
            val lectures = orchestra.lectures(sagresGroupId, 0, 0)
            if (lectures is Outcome.Success) {
                LectureProcessor(context, database, groupId, lectures.value, false).execute()
            } else if (lectures is Outcome.Error) {
                Timber.e(lectures.error, "Error during lectures. Code ${lectures.code}")
            }

            val absences = orchestra.absences(profile.sagresId, sagresGroupId, 0, 0)
            if (absences is Outcome.Success) {
                MissedLectureProcessor(context, database, profile.uid, groupId, absences.value, false).execute()
            } else if (absences is Outcome.Error) {
                Timber.e(absences.error, "Error during absences. Code ${absences.code}")
            }
        }
        emit(false)
    }

    @AnyThread
    fun loadClassDetails(groupId: Long): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        result.postValue(true)
        executors.networkIO().execute {
            Timber.d("Group id for load is $groupId")
            val access = database.accessDao().getAccessDirect()
            val value = database.classGroupDao().getWithRelationsDirect(groupId)
            if (value == null || access == null) {
                Timber.d("Class Group with ID: $groupId was not found")
                result.postValue(false)
            } else {
                val clazz = value.classData
                val semester = clazz.semester.name
                val code = clazz.discipline.code
                val group = value.group.group

                Timber.d("Code: $code. Semester: $semester. Group: $group")

                if (Constants.getParameter("REQUIRES_CAPTCHA") != "true") {
                    SagresNavigator.instance.login(access.username, access.password)
                }
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

    fun getMaterialsFromClassItem(classItemId: Long): LiveData<List<ClassMaterial>> {
        return database.classMaterialDao().getMaterialsFromClassItem(classItemId)
    }

    fun updateLocationVisibilityAsync(location: Long, status: Boolean) {
        executors.diskIO().execute {
            database.classLocationDao().setClassHiddenHidden(status, location)
        }
    }
}
