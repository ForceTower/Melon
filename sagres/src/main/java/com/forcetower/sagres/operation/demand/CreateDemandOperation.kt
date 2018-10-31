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
import com.forcetower.sagres.database.model.SDemandOffer
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.request.SagresCalls
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.concurrent.Executor

class CreateDemandOperation(
    private val revised: List<SDemandOffer>,
    executor: Executor?
): Operation<DemandCreatorCallback>(executor) {
    init {
        perform()
    }

    override fun execute() {
        val callback = loadOffers()?: return
        val document = callback.document!!
        val offers = callback.getOffers()!!
        val hash = offers.groupBy { it.code }

        revised.forEach {
            val list = hash[it.code]
            if (list == null || list.size != 1) {
                publishProgress(DemandCreatorCallback(Status.APPROVAL_ERROR).message("${it.code} was not identified on second pass. List size: ${list?.size}"))
                return
            }
            list[0].selected = it.selected
        }

        val list = hash.map { it.value[0] }
        val call = SagresCalls.createDemand(list, document)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                Timber.d("Request completed")
                val body = response.body()!!.string()
                val complete = createDocument(body)
                finalSteps(complete)
            } else {
                Timber.d("Response failed")
                publishProgress(DemandCreatorCallback(Status.RESPONSE_FAILED).code(response.code()))
            }
        } catch (t: Throwable) {
            Timber.d("Network error")
            publishProgress(DemandCreatorCallback(Status.NETWORK_ERROR).throwable(t))
        }
    }

    private fun loadOffers(): DemandOffersCallback? {
        val callback = SagresNavigator.instance.loadDemandOffers()
        val offers = callback.getOffers()
        val document = callback.document
        return when {
            callback.status != Status.SUCCESS -> {
                publishProgress(DemandCreatorCallback.copyFrom(callback))
                null
            }
            offers == null || document == null -> {
                publishProgress(DemandCreatorCallback(Status.UNKNOWN_FAILURE).message("Load demand had a null response"))
                null
            }
            else -> callback
        }
    }

    private fun finalSteps(complete: Document) {
        val element = complete.selectFirst("span[class=\"msg-sucesso anim-fadeIn\"]")
        if (element != null) {
            val text = element.text().trim()
            if (text.contains("O registro foi atualizado com sucesso", ignoreCase = true)) {
                publishProgress(DemandCreatorCallback(Status.SUCCESS).message(text))
            } else {
                publishProgress(DemandCreatorCallback(Status.COMPLETED).message(text))
            }
        } else {
            publishProgress(DemandCreatorCallback(Status.UNKNOWN_FAILURE).message("Success message not found. It's possible it failed"))
        }
    }
}