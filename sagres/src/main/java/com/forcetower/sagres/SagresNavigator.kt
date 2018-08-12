package com.forcetower.sagres

import android.content.Context

import com.forcetower.sagres.database.SagresDatabase
import com.forcetower.sagres.impl.SagresNavigatorImpl
import com.forcetower.sagres.operation.calendar.CalendarCallback
import com.forcetower.sagres.operation.login.LoginCallback
import com.forcetower.sagres.operation.messages.MessagesCallback
import com.forcetower.sagres.operation.person.PersonCallback
import com.forcetower.sagres.operation.start_page.StartPageCallback

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData

abstract class SagresNavigator {
    abstract val database: SagresDatabase

    @AnyThread
    abstract fun aLogin(username: String, password: String): LiveData<LoginCallback>

    @WorkerThread
    abstract fun login(username: String, password: String): SagresNavigator?

    @AnyThread
    abstract fun aMe(): LiveData<PersonCallback>

    @WorkerThread
    abstract fun me(): SagresNavigator?

    @AnyThread
    abstract fun aMessages(userId: Long): LiveData<MessagesCallback>

    @AnyThread
    abstract fun aCalendar(): LiveData<CalendarCallback>

    @AnyThread
    abstract fun startPage(): LiveData<StartPageCallback>

    abstract fun stopTags(tag: String)

    companion object {
        val instance: SagresNavigator
            get() = SagresNavigatorImpl.getInstance()

        fun initialize(context: Context) {
            SagresNavigatorImpl.initialize(context)
        }
    }
}
