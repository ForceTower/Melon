/*
 * Copyright (c) 2019.
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

import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SPerson
import com.forcetower.sagres.database.model.SDisciplineMissedClass
import com.forcetower.sagres.database.model.SGrade
import com.forcetower.sagres.database.model.SDisciplineClassLocation
import com.forcetower.sagres.database.model.SDiscipline
import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.sagres.database.model.SCalendar
import com.forcetower.sagres.database.model.SRequestedService
import com.forcetower.sagres.operation.Callback
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresBasicParser
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.CalendarItem
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.model.unes.ServiceRequest
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.core.work.grades.GradesSagresWorker
import com.forcetower.uefs.core.work.hourglass.HourglassContributeWorker
import org.jsoup.nodes.Document
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginSagresRepository @Inject constructor(
    private val executor: AppExecutors,
    private val database: UDatabase,
    private val preferences: SharedPreferences,
    private val firebaseAuthRepository: FirebaseAuthRepository
) {
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
            currentStep.value = createStep(R.string.step_delete_database)
            executor.diskIO().execute {
                database.accessDao().deleteAll()
                database.messageDao().deleteAll()
                database.accessTokenDao().deleteAll()
                database.calendarDao().delete()
                database.profileDao().deleteMe()
                database.semesterDao().deleteAll()
                executor.mainThread().execute {
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
        currentStep.value = createStep(R.string.step_logging_in)
        data.addSource(source) { l ->
            if (l.status == Status.SUCCESS) {
                data.removeSource(source)
                val score = SagresBasicParser.getScore(l.document)
                Timber.d("Login Completed. Score parsed: $score")
                executor.diskIO().execute { database.accessDao().insert(username, password) }
                me(data, score, Access(username = username, password = password), l.document!!)
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

    private fun me(data: MediatorLiveData<Callback>, score: Double, access: Access, document: Document) {
        val me = SagresNavigator.instance.aMe()
        currentStep.value = createStep(R.string.step_finding_profile)
        val username = access.username

        if (username.contains("@")) {
            continueUsingHtml(document, username, score, access, data)
        } else {
            data.addSource(me) { m ->
                Timber.d("Me status ${m.status} ${m.code}")
                if (m.status == Status.SUCCESS) {
                    data.removeSource(me)
                    Timber.d("Me Completed. You are ${m.person?.name} and your CPF is ${m.person?.cpf}")
                    val person = m.person
                    if (person != null) {
                        executor.diskIO().execute { database.profileDao().insert(person, score) }
                        executor.others().execute { firebaseAuthRepository.loginToFirebase(person, access, true) }
                        messages(data, person.id)
                    } else {
                        Timber.d("SPerson is null")
                    }
                } else if (m.status == Status.RESPONSE_FAILED) {
                    continueUsingHtml(document, username, score, access, data)
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
    }

    private fun continueUsingHtml(document: Document, username: String, score: Double, access: Access, data: MediatorLiveData<Callback>) {
        val name = SagresBasicParser.getName(document) ?: username
        val person = SPerson(username.hashCode().toLong(), name, name, "00000000000", username)
        executor.diskIO().execute { database.profileDao().insert(person, score) }
        executor.others().execute { firebaseAuthRepository.loginToFirebase(person, access, true) }
        messages(data, null)
    }

    private fun messages(data: MediatorLiveData<Callback>, userId: Long?) {
        val messages = if (userId != null)
            SagresNavigator.instance.aMessages(userId)
        else
            SagresNavigator.instance.aMessagesHtml()

        currentStep.value = createStep(R.string.step_fetching_messages)
        data.addSource(messages) { m ->
            if (m.status == Status.SUCCESS) {
                data.removeSource(messages)
                val values = m.messages!!.map { Message.fromMessage(it, true) }
                executor.diskIO().execute { database.messageDao().insertIgnoring(values) }
                Timber.d("Messages Completed")
                Timber.d("You got: ${m.messages}")
                if (userId != null)
                    semesters(data, userId)
                else
                    disciplinesExperimental(data)
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
        currentStep.value = createStep(R.string.step_fetching_semesters)
        data.addSource(semesters) { s ->
            if (s.status == Status.SUCCESS) {
                data.removeSource(semesters)
                val values = s.getSemesters().map { Semester.fromSagres(it) }
                executor.diskIO().execute { database.semesterDao().insertIgnoring(values) }
                Timber.d("Semesters Completed")
                Timber.d("You got: ${s.getSemesters()}")
                disciplinesExperimental(data)
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

    private fun disciplinesExperimental(data: MediatorLiveData<Callback>) {
        Timber.d("Disciplines Experimental")
        val experimental = SagresNavigator.instance.aDisciplinesExperimental()
        currentStep.value = createStep(R.string.step_discipline_experimental)
        data.addSource(experimental) { e ->
            Timber.d("Experimental Status: ${e.status}")
            if (e.status == Status.COMPLETED) {
                data.removeSource(experimental)
                executor.diskIO().execute {
                    defineSemesters(e.getSemesters())
                    defineDisciplines(e.getDisciplines())
                    defineDisciplineGroups(e.getGroups())
                }
                defineExperimentalWorkers()
                startPage(data)
            }
        }
    }

    private fun defineExperimentalWorkers() {
        if (preferences.isStudentFromUEFS())
            HourglassContributeWorker.createWorker()
        preferences.edit().putBoolean("sent_hourglass_testing_data_0.0.0", true).apply()
    }

    private fun startPage(data: MediatorLiveData<Callback>) {
        val start = SagresNavigator.instance.aStartPage()
        currentStep.value = createStep(R.string.step_moving_to_start_page)
        data.addSource(start) { s ->
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
        Timber.d("Grades Fetch!")
        val grades = SagresNavigator.instance.aGetCurrentGrades()
        currentStep.value = createStep(R.string.step_fetching_grades)
        data.addSource(grades) { g ->
            when {
                g.status == Status.SUCCESS -> {
                    data.removeSource(grades)

                    Timber.d("Grades received: ${g.grades}")
                    Timber.d("Frequency: ${g.frequency}")
                    Timber.d("Semesters: ${g.semesters}")

                    executor.diskIO().execute {
                        defineSemesters(g.semesters)
                        defineGrades(g.grades)
                        defineFrequency(g.frequency)
                        database.gradesDao().markAllNotified()
                        Timber.d("Execute default")
                        defineGradesWorkers(g.semesters.map { pair -> pair.first })
                    }

                    services(data)
                }
                g.status == Status.LOADING -> {
                    data.value = Callback.Builder(g.status)
                            .code(g.code)
                            .message(g.message)
                            .throwable(g.throwable)
                            .document(g.document)
                            .build()
                }
                else -> {
                    Timber.d("Data status: ${g.status} ${g.code} ${g.throwable?.message}")
                    data.value = Callback.Builder(Status.GRADES_FAILED)
                            .code(g.code)
                            .message(g.message)
                            .throwable(g.throwable)
                            .document(g.document)
                            .build()
                }
            }
        }
    }

    @MainThread
    private fun services(data: MediatorLiveData<Callback>) {
        Timber.d("Services fetch")
        val services = SagresNavigator.instance.aGetRequestedServices()
        data.addSource(services) { s ->
            when (s.status) {
                Status.SUCCESS -> {
                    data.removeSource(services)
                    executor.diskIO().execute {
                        defineServices(s.services)
                        database.serviceRequestDao().markAllNotified()
                    }
                    data.value = Callback.Builder(s.status).document(s.document).build()
                }
                Status.LOADING -> {
                    data.value = Callback.Builder(s.status)
                            .code(s.code)
                            .message(s.message)
                            .throwable(s.throwable)
                            .document(s.document)
                            .build()
                }
                else -> {
                    data.value = Callback.Builder(Status.COMPLETED)
                            .code(s.code)
                            .message(s.message)
                            .throwable(s.throwable)
                            .document(s.document)
                            .build()
                }
            }
        }
    }

    private fun defineServices(services: List<SRequestedService>) {
        val list = services.map { ServiceRequest.fromSagres(it) }
        database.serviceRequestDao().insertList(list)
    }

    private fun defineGradesWorkers(semesters: List<Long>) {
        semesters.forEach {
            GradesSagresWorker.createWorker(it)
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
        database.classGroupDao().defineGroups(groups)
    }

    @WorkerThread
    private fun defineDisciplines(disciplines: List<SDiscipline>) {
        val values = disciplines.map { Discipline.fromSagres(it) }
        database.disciplineDao().insert(values)
        disciplines.forEach { database.classDao().insert(it, true) }
    }

    @WorkerThread
    private fun defineCalendar(calendar: List<SCalendar>?) {
        val values = calendar?.map { CalendarItem.fromSagres(it) }
        database.calendarDao().deleteAndInsert(values)
    }

    companion object {
        private var currentStep = 0
        private const val stepCount = 7

        private fun resetSteps() {
            currentStep = 0
        }

        private fun incSteps() {
            currentStep++
        }

        private fun createStep(@StringRes desc: Int): Step = Step(currentStep++, desc)
    }

    data class Step(val step: Int, @StringRes val res: Int) {
        val count = stepCount
    }
}
