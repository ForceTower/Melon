/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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