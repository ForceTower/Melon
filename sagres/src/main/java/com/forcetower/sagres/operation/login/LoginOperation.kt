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

package com.forcetower.sagres.operation.login

import androidx.annotation.AnyThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.MediatorLiveData
import com.forcetower.sagres.Utils.createDocument
import com.forcetower.sagres.database.model.SAccess
import com.forcetower.sagres.impl.SagresNavigatorImpl
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresBasicParser
import com.forcetower.sagres.request.SagresCalls
import com.forcetower.sagres.utils.ConnectedStates
import okhttp3.Response
import org.jsoup.nodes.Document
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LoginOperation @AnyThread
constructor(
    private val username: String,
    private val password: String,
    executor: Executor?
) : Operation<LoginCallback>(executor) {
    init {
        this.perform()
    }

    override fun execute() {
        result.postValue(LoginCallback.started())
        val call = SagresCalls.login(username, password)

        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body
                val string = body!!.string()
                resolveLogin(string, response)
            } else {
                var doc: Document? = null
                val body = response.body
                if (body != null) {
                    doc = createDocument(body.string())
                }
                finished = LoginCallback.Builder(Status.RESPONSE_FAILED).document(doc).code(response.code).build()
                result.postValue(finished)
            }
        } catch (e: IOException) {
            Timber.d("Message: %s", e.message)
            e.printStackTrace()
            finished = LoginCallback.Builder(Status.NETWORK_ERROR).throwable(e).build()
            result.postValue(finished)
        }
    }

    private fun resolveLogin(string: String, response: Response) {
        val document = createDocument(string)

        when (SagresBasicParser.isConnected(document)) {
            ConnectedStates.CONNECTED -> continueWithResolve(document, response)
            ConnectedStates.INVALID -> continueWithInvalidation(document)
            ConnectedStates.SESSION_TIMEOUT -> continueWithStopFlags(document)
            ConnectedStates.UNKNOWN -> continueWithUnknownFlags(document)
        }
    }

    private fun continueWithUnknownFlags(document: Document) {
        val callback = LoginCallback.Builder(Status.INVALID_LOGIN).code(500).document(document).build()
        publishProgress(callback)
    }

    private fun continueWithStopFlags(document: Document) {
        val callback = LoginCallback.Builder(Status.INVALID_LOGIN).code(440).document(document).build()
        publishProgress(callback)
    }

    private fun continueWithInvalidation(document: Document) {
        val callback = LoginCallback.Builder(Status.INVALID_LOGIN).code(401).document(document).build()
        publishProgress(callback)
    }

    private fun continueWithResolve(document: Document, response: Response) {
        if (SagresBasicParser.needApproval(document)) {
            result.postValue(LoginCallback.Builder(Status.LOADING).message("Need approval").code(200).build())
            approval(document, response)
        } else {
            successMeasures(document)
        }
    }

    private fun approval(approvalDocument: Document, oldResp: Response) {
        val call = SagresCalls.loginApproval(approvalDocument, oldResp)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val document = createDocument(response.body!!.string())
                successMeasures(document)
            } else {
                finished = LoginCallback.Builder(Status.APPROVAL_ERROR).code(response.code).build()
                result.postValue(finished)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            finished = LoginCallback.Builder(Status.NETWORK_ERROR).throwable(e).build()
            result.postValue(finished)
        }
    }

    private fun successMeasures(document: Document) {
        success = true

        val database = SagresNavigatorImpl.instance.database
        val access = database.accessDao().accessDirect
        val created = SAccess(username, password)
        if (access == null || access != created) database.accessDao().insert(created)

        finished = LoginCallback.Builder(Status.SUCCESS).document(document).build()
        result.postValue(finished)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun getResult(): MediatorLiveData<LoginCallback> {
        return result
    }
}
