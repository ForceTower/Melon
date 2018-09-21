package com.forcetower.uefs.core.storage.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.defineInDatabase
import com.forcetower.uefs.core.storage.database.UDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagesRepository @Inject constructor(
    val database: UDatabase,
    val executors: AppExecutors
) {
    fun getMessages() = database.messageDao().getAllMessages()

    fun fetchMessages(): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        executors.networkIO().execute {
            val bool = fetchMessagesCase()
            result.postValue(bool)
        }
        return result
    }

    @WorkerThread
    fun fetchMessagesCase(): Boolean {
        val profile = database.profileDao().selectMeDirect()
        return if (profile != null) {
            val messages = SagresNavigator.instance.messages(profile.sagresId)
            if (messages.status == Status.SUCCESS) {
                messages.messages.defineInDatabase(database)
                true
            } else {
                false
            }
        } else {
            false
        }
    }
}