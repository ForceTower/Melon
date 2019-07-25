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
                val body = response.body!!.string()
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
