package com.forcetower.unes.core.storage.repository

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.*
import com.forcetower.sagres.operation.Callback
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresBasicParser
import com.forcetower.unes.AppExecutors
import com.forcetower.unes.R
import com.forcetower.unes.core.model.Access
import com.forcetower.unes.core.model.CalendarItem
import com.forcetower.unes.core.model.Message
import com.forcetower.unes.core.model.Semester
import com.forcetower.unes.core.storage.database.UDatabase
import com.forcetower.unes.core.storage.network.UService
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.concurrent.timer

class UserRepository @Inject constructor(
        private val executor: AppExecutors,
        private val database: UDatabase,
        private val service: UService,
        context: Context
) {
    private val appToken = context.getString(R.string.app_service_token)

    fun getAccess(): LiveData<Access?> = database.accessDao().getAccess()

    fun stopCurrentLogin() {
        SagresNavigator.instance.stopTags("aLogin")
    }

    fun getProfileMe() = database.profileDao().selectMe()

    @MainThread
    fun login(username: String, password: String, deleteDatabase: Boolean = false): LiveData<Callback> {
        val signIn = MediatorLiveData<Callback>()
        if (deleteDatabase) {
            executor.diskIO().execute {
                database.clearAllTables()
                executor.mainThread().execute{
                    login(signIn, username, password)
                }
            }
        } else {
            login(signIn, username, password)
        }
        return signIn
    }

    @MainThread
    private fun login(data: MediatorLiveData<Callback>, username: String, password: String) {
        val source = SagresNavigator.instance.aLogin(username, password)
        data.addSource(source) { l ->
            if (l.status == Status.SUCCESS) {
                data.removeSource(source)
                Timber.d("Login Completed")
                val score = SagresBasicParser.getScore(l.document)
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
                    Timber.d("Person is null")
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
                Timber.d("Semesters: " + s.semesters)
                Timber.d("Disciplines: " + s.disciplines)
                Timber.d("Calendar: " + s.calendar)
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
        data.value = Callback.Builder(Status.SUCCESS).build()
    }

    @WorkerThread
    private fun defineSchedule(locations: List<DisciplineClassLocation>?) {
        if (locations == null) return
    }

    @WorkerThread
    private fun defineDisciplineGroups(groups: List<DisciplineGroup>) {

    }

    @WorkerThread
    private fun defineDisciplines(disciplines: List<Discipline>) {
        val values = disciplines.map { com.forcetower.unes.core.model.Discipline.fromSagres(it) }
        database.disciplineDao().insert(values)
    }

    @WorkerThread
    private fun defineCalendar(calendar: List<SagresCalendar>?) {
        val values = calendar?.map { CalendarItem.fromSagres(it) }
        database.calendarDao().deleteAndInsert(values)
    }


    private fun loginToService(person: Person, username: String, password: String) {
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
}
