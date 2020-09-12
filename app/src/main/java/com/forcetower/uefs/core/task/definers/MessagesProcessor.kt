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
