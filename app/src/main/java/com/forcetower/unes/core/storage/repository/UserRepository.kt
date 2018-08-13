package com.forcetower.unes.core.storage.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SagresAccess
import com.forcetower.sagres.operation.Callback
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.operation.login.LoginCallback
import com.forcetower.unes.AppExecutors
import com.forcetower.unes.core.model.Message
import com.forcetower.unes.core.model.Profile
import com.forcetower.unes.core.model.Semester
import com.forcetower.unes.core.storage.database.UDatabase
import timber.log.Timber
import javax.inject.Inject

class UserRepository @Inject constructor(
        private val executor: AppExecutors,
        private val database: UDatabase
) {

    fun getAccess(): LiveData<SagresAccess> = SagresNavigator.instance.database.accessDao().access

    fun login(username: String, password: String): LiveData<Callback> {
        val signIn = MediatorLiveData<Callback>()
        login(signIn, username, password)
        return signIn
    }

    private fun login(data: MediatorLiveData<Callback>, username: String, password: String) {
        val source = SagresNavigator.instance.aLogin(username, password)
        data.addSource(source) { l ->
            if (l.status == Status.SUCCESS) {
                data.removeSource(source)
                Timber.d("Login Completed")
                executor.diskIO().execute { database.accessDao().insert(username, password) }
                me(data)
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

    private fun me(data: MediatorLiveData<Callback>) {
        val me = SagresNavigator.instance.aMe()
        data.addSource(me) {m ->
            if (m.status == Status.SUCCESS) {
                data.removeSource(me)
                Timber.d("Me Completed. You are ${m.person?.name} and your CPF is ${m.person?.cpf}")
                val person = m.person
                if (person != null) {
                    executor.diskIO().execute { database.profileDao().insert(person) }
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
                val values = Stream.of(m.messages!!).map { Message.fromMessage(it) }.collect(Collectors.toList())

                executor.diskIO().execute { database.messageDao().insert(values) }
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
                val values = Stream.of(s.getSemesters()).map { Semester.fromSagres(it) }.collect(Collectors.toList())
                executor.diskIO().execute { database.semesterDao().insert(values) }
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
                //TODO Insert Data to Database
                Timber.d("Start Page completed")
                Timber.d("Semesters: " + s.semesters)
                Timber.d("Disciplines: " + s.disciplines)
                Timber.d("Calendar: " + s.calendar)
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

    fun stopCurrentLogin() {
        SagresNavigator.instance.stopTags("aLogin")
    }
}
