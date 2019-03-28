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
                val body = response.body()
                val string = body!!.string()
                resolveLogin(string, response)
            } else {
                var doc: Document? = null
                val body = response.body()
                if (body != null) {
                    doc = createDocument(body.string())
                }
                finished = LoginCallback.Builder(Status.RESPONSE_FAILED).document(doc).code(response.code()).build()
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
        val loginState = SagresBasicParser.isConnected(document)

        when (loginState) {
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
            result.postValue(LoginCallback.Builder(Status.LOADING).message("Need approval").build())
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
                val document = createDocument(response.body()!!.string())
                successMeasures(document)
            } else {
                finished = LoginCallback.Builder(Status.APPROVAL_ERROR).code(response.code()).build()
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
