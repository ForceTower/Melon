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
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SDiscipline
import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.operation.disciplines.FastDisciplinesCallback
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.service.discipline.DisciplineDetailsData
import com.forcetower.uefs.core.model.service.discipline.transformToNewStyle
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.resource.discipline.LoadDisciplineDetailsResource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisciplineDetailsRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors,
    private val gradesRepository: SagresGradesRepository,
    private val service: UService
) {

    /**
     * This is a funny function
     * It will load the details of any discipline that matches the requirements, if all the parameters
     * are null, it will load the details of every single discipline.
     *
     * This function may take several minutes to complete, it's better to be called within a service
     * or something that shows to the user that this long operation is running.
     *
     * Funny fact: It only saves the data once it has completed downloading everything, a network
     * connection fail during the process will generate a catastrophic fail.
     *
     * Note to myself: save the data when it is ready, don't wait until everything completes
     *
     * Second note: add a "don't load this discipline", kind of a blacklist so the process ignore the
     * discipline and load everything faster
     *
     * Third note: add a "simple load" and a "full load". Simple load will only fetch the teacher
     * name, which is the common use for this function.
     */
    @MainThread
    fun loadDisciplineDetails(semester: String? = null, code: String? = null, group: String? = null, partialLoad: Boolean = true, discover: Boolean = false): LiveData<FastDisciplinesCallback> {
        return object : LoadDisciplineDetailsResource(executors, semester, code, group, partialLoad, discover) {
            @WorkerThread
            override fun saveResults(callback: FastDisciplinesCallback) {
                defineSemesters(callback.getSemesters())
                defineDisciplines(callback.getDisciplines())
                defineDisciplineGroups(callback.getGroups())
            }

            @WorkerThread
            override fun loadGrades() {
                val semesters = database.semesterDao().getSemestersDirect()
                semesters.forEach { gradesRepository.getGrades(it.sagresId, false) }
            }
        }.asLiveData()
    }

    @AnyThread
    fun contribute() {
        executors.diskIO().execute {
            sendDisciplineDetails()
        }
    }

    @WorkerThread
    fun sendDisciplineDetails() {
        val stats = database.classGroupDao().getClassStatsWithAllDirect()
        val semesters = database.semesterDao().getSemestersDirect()
        val profile = database.profileDao().selectMeDirect() ?: return

        val treated = stats.transformToNewStyle()

        val amountSemesters = semesters.size
        val score = if (profile.score != -1.0) profile.score else profile.calcScore

        val data = DisciplineDetailsData(amountSemesters, score, profile.course, treated)
        try {
            val response = service.sendGrades(data).execute()
            if (response.isSuccessful) {
                Timber.d("Success Response")
                val result = response.body()!!
                Timber.d("Result ${result.success}")
            } else {
                Timber.d("Failed Response Code: ${response.code()}")
            }
        } catch (exception: Exception) {
            Timber.e(exception)
        }
    }

    @WorkerThread
    fun loadDisciplineDetailsSync(partialLoad: Boolean = true, notify: Boolean = false) {
        experimentalDisciplines(partialLoad, notify)
        val semesters = database.semesterDao().getSemestersDirect()
        semesters.forEach { gradesRepository.getGrades(it.sagresId, false) }
        sendDisciplineDetails()
    }

    @WorkerThread
    fun experimentalDisciplines(partialLoad: Boolean = false, notify: Boolean = true) {
        val experimental = SagresNavigator.instance.disciplinesExperimental(discover = false, partialLoad = partialLoad)
        if (experimental.status == Status.COMPLETED) {
            defineSemesters(experimental.getSemesters())
            defineDisciplines(experimental.getDisciplines())
            defineDisciplineGroups(experimental.getGroups(), notify)
        }
        database.classMaterialDao().markAllNotified()
    }

    @WorkerThread
    private fun defineSemesters(semesters: List<Pair<Long, String>>) {
        semesters.forEach {
            val semester = Semester(sagresId = it.first, name = it.second, codename = it.second)
            database.semesterDao().insertIgnoring(semester)
        }
    }

    @WorkerThread
    private fun defineDisciplines(disciplines: List<SDiscipline>) {
        val values = disciplines.map { Discipline.fromSagres(it) }
        database.disciplineDao().insert(values)
        disciplines.forEach { database.classDao().insert(it, true) }
    }

    @WorkerThread
    private fun defineDisciplineGroups(groups: List<SDisciplineGroup>, notify: Boolean = true) {
        database.classGroupDao().defineGroups(groups, notify)
    }
}