/*
 * Copyright (c) 2018.
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

package com.forcetower.sagres.operation.demand

import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.Utils.createDocument
import com.forcetower.sagres.database.model.SAccess
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresDemandParser
import com.forcetower.sagres.request.SagresCalls
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.concurrent.Executor

class LoadDemandOffersOperation(executor: Executor?) : Operation<DemandOffersCallback>(executor) {
    init {
        perform()
    }

    override fun execute() {
        val access = SagresNavigator.instance.database.accessDao().accessDirect
        if (access == null) {
            publishProgress(DemandOffersCallback(Status.INVALID_LOGIN).message("Invalid Access"))
            return
        }

        executeSteps(access)
    }

    private fun executeSteps(access: SAccess) {
        login(access) ?: return
        Timber.d("Connected for load demand offers")
        val document = demandPage() ?: return
        val offers = SagresDemandParser.getOffers(document)
        if (offers != null) {
            publishProgress(DemandOffersCallback(Status.SUCCESS).offers(offers).document(document))
        } else {
            publishProgress(DemandOffersCallback(Status.APPROVAL_ERROR).message("Not able to find the demand object").document(document))
        }
    }

    private fun demandPage(): Document? {
        val call = SagresCalls.getDemandPage()

        try {
            val response = call.execute()
            if (response.isSuccessful) {
                Timber.d("Completed request!")
                val body = response.body()!!.string()
                return createDocument(body)
            } else {
                Timber.d("Failed loading")
                publishProgress(DemandOffersCallback(Status.RESPONSE_FAILED).code(response.code()).message("Failed loading"))
            }
        } catch (t: Throwable) {
            Timber.d("Error loading page. Throwable message ${t.message}")
            publishProgress(DemandOffersCallback(Status.NETWORK_ERROR).throwable(t))
        }
        return null
    }

    private fun login(access: SAccess): BaseCallback<*>? {
        val login = SagresNavigator.instance.login(access.username, access.password)
        if (login.status != Status.SUCCESS) {
            publishProgress(DemandOffersCallback.copyFrom(login))
            return null
        }
        return login
    }
}