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
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SagresCalendar
import com.forcetower.sagres.database.model.SagresCredential
import com.forcetower.sagres.database.model.SagresDiscipline
import com.forcetower.sagres.database.model.SagresDisciplineClassLocation
import com.forcetower.sagres.database.model.SagresDisciplineGroup
import com.forcetower.sagres.database.model.SagresDisciplineMissedClass
import com.forcetower.sagres.database.model.SagresGrade
import com.forcetower.sagres.database.model.SagresMessage
import com.forcetower.sagres.database.model.SagresPerson
import com.forcetower.sagres.database.model.SagresRequestedService
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
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository
import com.forcetower.uefs.core.util.LocationShrinker
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.core.util.toLiveData
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
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val authRepository: AuthRepository,
    private val sessionRepository: CookieSessionRepository,
    private val context: Context
) {
    val currentStep: MutableLiveData<Step> = MutableLiveData()

    fun getAccess(): LiveData<Access?> = database.accessDao().getAccess()

    fun stopCurrentLogin() {
        SagresNavigator.instance.stopTags("aLogin")
    }

    fun getProfileMe() = database.profileDao().selectMe()

    @MainThread
    fun login(username: String, password: String, captcha: String?, deleteDatabase: Boolean = false, skipLogin: Boolean = false): LiveData<Callback> {
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
                    login(signIn, username, password, captcha, skipLogin)
                }
            }
        } else {
            incSteps()
            login(signIn, username, password, captcha, skipLogin)
        }
        return signIn
    }

    @MainThread
    private fun login(data: MediatorLiveData<Callback>, username: String, password: String, captcha: String?, skipLogin: Boolean) {
        SagresNavigator.instance.putCredentials(SagresCredential(username, password, SagresNavigator.instance.getSelectedInstitution()))

        val source = if (!skipLogin) {
            currentStep.value = createStep(R.string.step_logging_in)
            SagresNavigator.instance.aLogin(username, password, captcha).toLiveData()
        } else {
            currentStep.value = createStep(R.string.step_login_bypassed)
            SagresNavigator.instance.aStartPage().toLiveData()
        }
        data.addSource(source) { l ->
            if (l.status == Status.SUCCESS) {
                data.removeSource(source)
                val score = SagresBasicParser.getScore(l.document)
                Timber.d("Login Completed. Score parsed: $score")
                executor.diskIO().execute {
                    database.accessDao().insert(username, password)
                    if (preferences.isStudentFromUEFS()) {
                        authRepository.syncLogin(username, password)
                        sessionRepository.onLogin()
                    }
                }
                me(data, score, Access(username = username, password = password), l.document!!)
            } else {
                SagresNavigator.instance.putCredentials(null)
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
        currentStep.value = createStep(R.string.step_finding_profile)
        val username = access.username

        if (username.contains("@") || !preferences.isStudentFromUEFS()) {
            continueUsingHtml(document, username, score, access, data)
        } else {
            val me = SagresNavigator.instance.aMe().toLiveData()
            data.addSource(me) { m ->
                Timber.d("Me status ${m.status} ${m.code} ${m.throwable}")
                if (m.status == Status.SUCCESS) {
                    data.removeSource(me)
                    Timber.d("Me Completed. You are ${m.person?.name} and your CPF is ${m.person?.getCpf()}")
                    val person = m.person
                    if (person != null) {
                        executor.diskIO().execute { database.profileDao().insert(person, score) }
                        executor.others().execute { firebaseAuthRepository.loginToFirebase(person, access, true) }
                        messages(data, person.id)
                    } else {
                        Timber.d("SPerson is null")
                    }
                } else if (m.status == Status.RESPONSE_FAILED || m.status == Status.NETWORK_ERROR) {
                    m.throwable?.printStackTrace()
                    continueUsingHtml(document, username, score, access, data)
                } else {
                    Timber.d("The status ${m.status}")
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
        val person = SagresPerson(username.hashCode().toLong(), name, name, "00000000000", username)
        executor.diskIO().execute { database.profileDao().insert(person, score) }
        executor.others().execute { firebaseAuthRepository.loginToFirebase(person, access, true) }
        messages(data, null)
    }

    private fun messages(data: MediatorLiveData<Callback>, userId: Long?) {
        val messages = if (userId != null)
            SagresNavigator.instance.aMessages(userId).toLiveData()
        else
            SagresNavigator.instance.aMessagesHtml().toLiveData()

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
        val semesters = SagresNavigator.instance.aSemesters(userId).toLiveData()
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
        val experimental = SagresNavigator.instance.aDisciplinesExperimental().toLiveData()
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
            HourglassContributeWorker.createWorker(context)
        preferences.edit().putBoolean("sent_hourglass_testing_data_0.0.0", true).apply()
    }

    private fun startPage(data: MediatorLiveData<Callback>) {
        val start = SagresNavigator.instance.aStartPage().toLiveData()
        currentStep.value = createStep(R.string.step_moving_to_start_page)
        data.addSource(start) { s ->
            if (s.status == Status.SUCCESS) {
                data.removeSource(start)

                executor.diskIO().execute {
                    defineMessages(s.messages)
                    defineCalendar(s.calendar)
                    defineDisciplines(s.disciplines)
                    defineDisciplineGroups(s.groups)
                    defineSchedule(s.locations)
                }

                Timber.d("Start Page completed")
                Timber.d("Semesters: ${s.semesters}")
                Timber.d("Disciplines: ${s.disciplines}")
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
        val grades = SagresNavigator.instance.aGetCurrentGrades().toLiveData()
        currentStep.value = createStep(R.string.step_fetching_grades)
        data.addSource(grades) { g ->
            when (g.status) {
                Status.SUCCESS -> {
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
                        val semesters = g.semesters?.map { pair -> pair.first } ?: emptyList()
                        defineGradesWorkers(semesters)
                    }

                    services(data)
                }
                Status.LOADING -> {
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
        val services = SagresNavigator.instance.aGetRequestedServices().toLiveData()
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
                    Timber.d("ANOTHER ONE!")
                    executor.networkIO().execute { sessionRepository.onLogin() }
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

    private fun defineServices(services: List<SagresRequestedService>) {
        val list = services.map { ServiceRequest.fromSagres(it) }
        database.serviceRequestDao().insertList(list)
    }

    private fun defineGradesWorkers(semesters: List<Long>) {
        semesters.forEach {
            GradesSagresWorker.createWorker(context, it)
        }
    }

    @WorkerThread
    private fun defineFrequency(frequency: List<SagresDisciplineMissedClass>?) {
        if (frequency == null) return
        database.classAbsenceDao().putAbsences(frequency)
    }

    @WorkerThread
    private fun defineGrades(grades: List<SagresGrade>?) {
        grades?.run {
            database.gradesDao().putGrades(grades)
        }
    }

    @WorkerThread
    private fun defineSemesters(semesters: List<Pair<Long, String>>?) {
        semesters?.forEach {
            val semester = Semester(sagresId = it.first, name = it.second, codename = it.second)
            database.semesterDao().insertIgnoring(semester)
        }
    }

    @WorkerThread
    private fun defineSchedule(locations: List<SagresDisciplineClassLocation>?) {
        if (locations == null) return
        val ordering = preferences.getBoolean("stg_semester_deterministic_ordering", true)
        val shrinkSchedule = preferences.getBoolean("stg_schedule_shrinking", true)
        if (shrinkSchedule) {
            val shrink = LocationShrinker.shrink(locations)
            database.classLocationDao().putSchedule(shrink, ordering)
        } else {
            database.classLocationDao().putSchedule(locations, ordering)
        }
    }

    @WorkerThread
    private fun defineDisciplineGroups(groups: List<SagresDisciplineGroup>?) {
        groups ?: return
        database.classGroupDao().defineGroups(groups)
    }

    @WorkerThread
    private fun defineDisciplines(disciplines: List<SagresDiscipline>?) {
        disciplines ?: return
        val values = disciplines.map { Discipline.fromSagres(it) }
        database.disciplineDao().insert(values)
        disciplines.forEach { database.classDao().insert(it, true) }
    }

    @WorkerThread
    private fun defineCalendar(calendar: List<SagresCalendar>?) {
        val values = calendar?.map { CalendarItem.fromSagres(it) }
        database.calendarDao().deleteAndInsert(values)
    }

    private fun defineMessages(messages: List<SagresMessage>?) {
        messages ?: return
        messages.reversed().forEachIndexed { index, message ->
            message.processingTime = System.currentTimeMillis() + index
        }
        val values = messages.map { Message.fromMessage(it, true) }
        database.messageDao().insertIgnoring(values)
    }

    companion object {
        private var currentStep = 0
        private const val stepCount = 7

        fun resetSteps() {
            currentStep = 0
        }

        fun incSteps() {
            currentStep++
        }

        fun createStep(@StringRes desc: Int): Step = Step(currentStep++, desc)
    }

    data class Step(val step: Int, @StringRes val res: Int) {
        val count = stepCount
    }
}
