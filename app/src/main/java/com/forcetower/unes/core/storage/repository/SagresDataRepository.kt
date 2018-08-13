package com.forcetower.unes.core.storage.repository

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.forcetower.unes.core.model.Message
import com.forcetower.unes.core.storage.database.UDatabase
import javax.inject.Inject

class SagresDataRepository
@Inject
constructor(private val database: UDatabase) {

    fun getMessages(): LiveData<PagedList<Message>> {
        val config = PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(10)
                .build()

        val dataSource = database.messageDao().getAllMessages()
        return LivePagedListBuilder(dataSource, config).build()
    }
}