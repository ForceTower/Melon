/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.sagres.operation.messages

import com.forcetower.sagres.Utils.createDocument
import com.forcetower.sagres.impl.SagresNavigatorImpl
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresMessageParser
import com.forcetower.sagres.request.SagresCalls
import java.util.concurrent.Executor

class OldMessagesOperation(
    executor: Executor?,
    private val needsAuth: Boolean = false
) : Operation<MessagesCallback>(executor) {

    init { this.perform() }

    override fun execute() {
        publishProgress(MessagesCallback(Status.LOADING))
        if (needsAuth) {
            val access = SagresNavigatorImpl.instance.database.accessDao().accessDirect
            if (access == null) {
                publishProgress(MessagesCallback(Status.INVALID_LOGIN))
                return
            }

            SagresNavigatorImpl.instance.login(access.username, access.password)
        }

        val call = SagresCalls.startPage
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                processResponse(response.body!!.string())
            } else {
                publishProgress(MessagesCallback(Status.RESPONSE_FAILED))
            }
        } catch (e: Exception) {
            publishProgress(MessagesCallback(Status.NETWORK_ERROR))
        }
    }

    private fun processResponse(response: String) {
        val document = createDocument(response)
        val messages = SagresMessageParser.getMessages(document)
        messages.reversed().forEachIndexed { index, message ->
            message.processingTime = System.currentTimeMillis() + index
        }
        publishProgress(MessagesCallback(Status.SUCCESS).messages(messages))
    }
}