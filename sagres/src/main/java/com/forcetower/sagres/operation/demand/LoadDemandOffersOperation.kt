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
                val body = response.body!!.string()
                return createDocument(body)
            } else {
                Timber.d("Failed loading")
                publishProgress(DemandOffersCallback(Status.RESPONSE_FAILED).code(response.code).message("Failed loading"))
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