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
) : Operation<DemandCreatorCallback>(executor) {
    init {
        perform()
    }

    override fun execute() {
        val callback = loadOffers() ?: return
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
                val body = response.body!!.string()
                val complete = createDocument(body)
                finalSteps(complete)
            } else {
                Timber.d("Response failed")
                publishProgress(DemandCreatorCallback(Status.RESPONSE_FAILED).code(response.code))
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