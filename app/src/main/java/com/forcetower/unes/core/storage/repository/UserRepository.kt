package com.forcetower.unes.core.storage.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SagresAccess
import com.forcetower.sagres.operation.Callback
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.operation.login.LoginCallback
import com.forcetower.unes.AppExecutors
import timber.log.Timber
import javax.inject.Inject

class UserRepository @Inject constructor(
        private val executor: AppExecutors
) {

    fun getAccess(): LiveData<SagresAccess> = SagresNavigator.getInstance().database.accessDao().access

    fun login(username: String, password: String): LiveData<Callback> {
        val signIn = MediatorLiveData<Callback>()
        val login = SagresNavigator.getInstance().aLogin(username, password)
        login(signIn, login)
        return signIn
    }

    private fun login(data: MediatorLiveData<Callback>, source: LiveData<LoginCallback>) {
        data.addSource(source) { l ->
            if (l.status == Status.SUCCESS) {
                data.removeSource(source)
                Timber.d("Login Completed")
                //TODO Insert Data to Database
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
        val me = SagresNavigator.getInstance().aMe()
        data.addSource(me) {m ->
            if (m.status == Status.SUCCESS) {
                data.removeSource(me)
                Timber.d("Me Completed. You are ${m.person?.name} and your CPF is ${m.person?.cpf}")
                //TODO Insert Data to Database
                messages(data, m.person?.id!!)
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
        val messages = SagresNavigator.getInstance().aMessages(userId)
        data.addSource(messages) { m ->
            if (m.status == Status.SUCCESS) {
                data.removeSource(messages)
                //TODO Insert Data to Database
                Timber.d("Messages Completed")
                Timber.d("You got: ${m.messages}")
                startPage(data)
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

    private fun startPage(data: MediatorLiveData<Callback>) {
        val start = SagresNavigator.getInstance().startPage()
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
        SagresNavigator.getInstance().stopTags("aLogin")
    }
}
