package com.forcetower.uefs.core.task.definers

import android.content.Context
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.notify
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.UTask
import dev.forcetower.breaker.model.MessagesDataPage

class MessagesProcessor(
    private val page: MessagesDataPage,
    private val database: UDatabase,
    private val context: Context,
    private val notified: Boolean = false
) : UTask {
    override suspend fun execute() {
        val messages = page.messages
        database.messageDao().insertIgnoring(messages.map { Message.fromMessage(it, notified) })

        val newMessages = database.messageDao().getNewMessages()
        database.messageDao().setAllNotified()
        newMessages.forEach { it.notify(context) }
    }
}