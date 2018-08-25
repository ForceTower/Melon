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

package com.forcetower.unes.core.storage.repository

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.*
import com.forcetower.sagres.operation.Callback
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresBasicParser
import com.forcetower.unes.AppExecutors
import com.forcetower.unes.R
import com.forcetower.unes.core.model.*
import com.forcetower.unes.core.storage.database.UDatabase
import com.forcetower.unes.core.storage.network.UService
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class LoginSagresRepository @Inject constructor(
        private val executor: AppExecutors,
        private val database: UDatabase,
        private val service: UService,
        private val context: Context
) {
    private val appToken = context.getString(R.string.app_service_token)
    val currentStep: MutableLiveData<Step> = MutableLiveData()

    fun getAccess(): LiveData<Access?> = database.accessDao().getAccess()

    fun stopCurrentLogin() {
        SagresNavigator.instance.stopTags("aLogin")
    }

    fun getProfileMe() = database.profileDao().selectMe()

    @MainThread
    fun login(username: String, password: String, deleteDatabase: Boolean = false): LiveData<Callback> {
        val signIn = MediatorLiveData<Callback>()
        resetSteps()
        if (deleteDatabase) {
            currentStep.value = createStep(context, R.string.step_delete_database)
            executor.diskIO().execute {
                database.clearAllTables()
                executor.mainThread().execute{
                    login(signIn, username, password)
                }
            }
        } else {
            incSteps()
            login(signIn, username, password)
        }
        return signIn
    }

    @MainThread
    private fun login(data: MediatorLiveData<Callback>, username: String, password: String) {
        val source = SagresNavigator.instance.aLogin(username, password)
        currentStep.value = createStep(context, R.string.step_logging_in)
        data.addSource(source) { l ->
            if (l.status == Status.SUCCESS) {
                data.removeSource(source)
                val score = SagresBasicParser.getScore(l.document)
                Timber.d("Login Completed. Score parsed: $score")
                executor.diskIO().execute { database.accessDao().insert(username, password) }
                me(data, score, username, password)
            } else {
                data.value = Callback.Builder(l.status)
                        .code(l.code)
                        .message(l.message)
                        .throwable(l.throwable)
                        .document(l.document)
                        .build()
            }
        }
    }

    private fun me(data: MediatorLiveData<Callback>, score: Double, username: String, password: String) {
        val me = SagresNavigator.instance.aMe()
        currentStep.value = createStep(context, R.string.step_finding_profile)
        data.addSource(me) {m ->
            if (m.status == Status.SUCCESS) {
                data.removeSource(me)
                Timber.d("Me Completed. You are ${m.person?.name} and your CPF is ${m.person?.cpf}")
                val person = m.person
                if (person != null) {
                    executor.diskIO().execute { database.profileDao().insert(person, score) }
                    executor.networkIO().execute { loginToService(person, username, password) }
                    messages(data, person.id)
                } else {
                    Timber.d("SPerson is null")
                }
            } else {
                data.value = Callback.Builder(m.status)
                        .code(m.code)
                        .message(m.message)
                        .throwable(m.throwable)
                        .document(m.document)
                        .build()
            }
        }
    }

    private fun messages(data: MediatorLiveData<Callback>, userId: Long) {
        val messages = SagresNavigator.instance.aMessages(userId)
        currentStep.value = createStep(context, R.string.step_fetching_messages)
        data.addSource(messages) { m ->
            if (m.status == Status.SUCCESS) {
                data.removeSource(messages)
                val values = m.messages!!.map { Message.fromMessage(it) }
                executor.diskIO().execute { database.messageDao().insertIgnoring(values) }
                Timber.d("Messages Completed")
                Timber.d("You got: ${m.messages}")
                semesters(data, userId)
            } else {
                data.value = Callback.Builder(m.status)
                        .code(m.code)
                        .message(m.message)
                        .throwable(m.throwable)
                        .document(m.document)
                        .build()
            }
        }
    }

    private fun semesters(data: MediatorLiveData<Callback>, userId: Long) {
        val semesters = SagresNavigator.instance.aSemesters(userId)
        currentStep.value = createStep(context, R.string.step_fetching_semesters)
        data.addSource(semesters) {s ->
            if (s.status == Status.SUCCESS) {
                data.removeSource(semesters)
                val values = s.getSemesters().map { Semester.fromSagres(it) }
                executor.diskIO().execute { database.semesterDao().insertIgnoring(values) }
                Timber.d("Semesters Completed")
                Timber.d("You got: ${s.getSemesters()}")
                startPage(data)
            } else {
                data.value = Callback.Builder(s.status)
                        .code(s.code)
                        .message(s.message)
                        .throwable(s.throwable)
                        .document(s.document)
                        .build()
            }
        }
    }

    private fun startPage(data: MediatorLiveData<Callback>) {
        val start = SagresNavigator.instance.startPage()
        currentStep.value = createStep(context, R.string.step_moving_to_start_page)
        data.addSource(start){s ->
            if (s.status == Status.SUCCESS) {
                data.removeSource(start)

                executor.diskIO().execute {
                    defineCalendar(s.calendar)
                    defineDisciplines(s.disciplines)
                    defineDisciplineGroups(s.groups)
                    defineSchedule(s.locations)
                }

                Timber.d("Start Page completed")
                Timber.d("Semesters: ${s.semesters}")
                Timber.d("Disciplines:  ${s.disciplines}")
                Timber.d("Calendar: ${s.calendar}")
                grades(data)
            } else {
                data.value = Callback.Builder(s.status)
                        .code(s.code)
                        .message(s.message)
                        .throwable(s.throwable)
                        .document(s.document)
                        .build()
            }
        }
    }

    private fun grades(data: MediatorLiveData<Callback>) {
        val grades = SagresNavigator.instance.getCurrentGrades()
        currentStep.value = createStep(context, R.string.step_fetching_grades)
        data.addSource(grades){g ->
            if (g.status == Status.SUCCESS) {
                data.removeSource(grades)

                Timber.d("Grades received: ${g.grades}")
                Timber.d("Frequency: ${g.frequency}")
                Timber.d("Semesters: ${g.semesters}")
                executor.diskIO().execute {
                    defineSemesters(g.semesters)
                    defineGrades(g.grades)
                    defineFrequency(g.frequency)
                    data.postValue(Callback.Builder(g.status).document(g.document).build())
                }
            } else {
                data.value = Callback.Builder(g.status)
                        .code(g.code)
                        .message(g.message)
                        .throwable(g.throwable)
                        .document(g.document)
                        .build()
            }
        }
    }

    @WorkerThread
    private fun defineFrequency(frequency: List<SDisciplineMissedClass>?) {
        if (frequency == null) return
        database.classAbsenceDao().putAbsences(frequency)
    }

    @WorkerThread
    private fun defineGrades(grades: List<SGrade>) {
        database.gradesDao().putGrades(grades)
    }

    @WorkerThread
    private fun defineSemesters(semesters: List<Pair<Long, String>>) {
        semesters.forEach {
            val semester = Semester(sagresId = it.first, name = it.second, codename = it.second)
            database.semesterDao().insertIgnoring(semester)
        }
    }

    @WorkerThread
    private fun defineSchedule(locations: List<SDisciplineClassLocation>?) {
        if (locations == null) return
        database.classLocationDao().putSchedule(locations)
    }

    @WorkerThread
    private fun defineDisciplineGroups(groups: List<SDisciplineGroup>) {
        val values = ArrayList<ClassGroup>()
        groups.forEach {
            val group = database.classGroupDao().insert(it)
            values.add(group)
        }
        database.classStudentDao().joinGroups(values)
    }

    @WorkerThread
    private fun defineDisciplines(disciplines: List<SDiscipline>) {
        val values = disciplines.map { com.forcetower.unes.core.model.Discipline.fromSagres(it) }
        database.disciplineDao().insert(values)
        disciplines.forEach { database.classDao().insert(it) }
    }

    @WorkerThread
    private fun defineCalendar(calendar: List<SCalendar>?) {
        val values = calendar?.map { CalendarItem.fromSagres(it) }
        database.calendarDao().deleteAndInsert(values)
    }


    private fun loginToService(person: SPerson, username: String, password: String) {
        val login = service.loginOrCreate(username, password, person.email, person.name, person.cpf, appToken)
        try {
            val response = login.execute()
            if (response.isSuccessful) {
                val token = response.body()
                if (token != null) {
                    database.accessTokenDao().insert(token)
                } else {
                    Timber.d("Access Token is null")
                }
            } else {
                Timber.d("Response is unsuccessful. The code is: ${response.code()}")
            }
        } catch (e: IOException) {
            Timber.d("Request Failed")
            e.printStackTrace()
        }
    }

    companion object {
        private var currentStep = 0
        private const val stepCount = 6

        private fun resetSteps() {
            currentStep = 0
        }

        private fun incSteps() {
            currentStep++
        }

        private fun createStep(ctx: Context, @StringRes desc: Int): Step = Step(currentStep++, desc)
                //ctx.getString(R.string.data_step_format, currentStep++, stepCount, ctx.getString(desc))
    }

    data class Step(val step: Int, @StringRes val res: Int) {
        val count = stepCount
    }
}
