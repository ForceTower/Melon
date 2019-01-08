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

package com.forcetower.sagres.operation.servicerequest

import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.Utils.createDocument
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresRequestedServicesParser
import com.forcetower.sagres.request.SagresCalls
import org.jsoup.nodes.Document
import timber.log.Timber
import java.lang.Exception
import java.util.concurrent.Executor

class RequestedServicesOperation(
    executor: Executor?,
    private val login: Boolean = false
) : Operation<RequestedServicesCallback>(executor) {
    init {
        this.perform()
    }

    override fun execute() {
        publishProgress(RequestedServicesCallback(Status.STARTED))

        if (!login) afterLogin()
        else {
            val access = SagresNavigator.instance.database.accessDao().accessDirect
            if (access == null) {
                publishProgress(RequestedServicesCallback(Status.INVALID_LOGIN))
            } else {
                val result = SagresNavigator.instance.login(access.username, access.password)
                if (result.status == Status.SUCCESS)
                    afterLogin()
                else {
                    Timber.d("Invalid login status: ${result.status}")
                    publishProgress(RequestedServicesCallback(result.status))
                }
            }
        }
    }

    private fun afterLogin() {
        val call = SagresCalls.getRequestedServices()
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()!!.string()
                val document = createDocument(body)
                successMeasures(document)
            } else {
                publishProgress(RequestedServicesCallback(Status.RESPONSE_FAILED))
            }
        } catch (e: Exception) {
            publishProgress(RequestedServicesCallback(Status.NETWORK_ERROR).throwable(e))
        }
    }

    private fun successMeasures(document: Document) {
        val services = SagresRequestedServicesParser.extractRequestedServices(document)
        Timber.d("Services requested: ${services.size}")
        publishProgress(RequestedServicesCallback(Status.SUCCESS).services(services))
    }
}
