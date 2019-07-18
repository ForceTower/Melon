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

package com.forcetower.uefs.core.storage.repository

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.crashlytics.android.Crashlytics
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SDemandOffer
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.work.demand.CreateDemandWorker
import com.forcetower.uefs.service.NotificationCreator
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemandRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors,
    private val context: Context,
    private val analytics: FirebaseAnalytics
) {

    fun loadDemand(): LiveData<Resource<List<SDemandOffer>>> {
        val result = MediatorLiveData<Resource<List<SDemandOffer>>>()

        result.value = Resource.loading(null)
        val local = database.demandOfferDao().getAll()
        val network = SagresNavigator.instance.aLoadDemandOffers()

        result.addSource(local) { result.value = Resource.loading(it) }

        result.addSource(network) {
            Timber.d("Updated to status ${it.status}")
            when (it.status) {
                Status.SUCCESS -> {
                    result.removeSource(local)
                    result.removeSource(network)

                    executors.diskIO().execute {
                        database.demandOfferDao().defineDemandOffers(it.getOffers()!!)
                    }
                    result.addSource(local) { complete -> result.value = Resource.success(complete) }
                }
                Status.INVALID_LOGIN -> result.removeSource(network)
                Status.NETWORK_ERROR -> result.removeSource(network)
                Status.RESPONSE_FAILED -> result.removeSource(network)
                Status.APPROVAL_ERROR -> result.removeSource(network)
                Status.UNKNOWN_FAILURE -> result.removeSource(network)
                Status.COMPLETED -> result.removeSource(network)
                else -> Unit
            }
        }

        return result
    }

    fun getSelected() = database.demandOfferDao().getSelected()

    fun updateOfferSelection(offer: SDemandOffer, select: Boolean) {
        executors.diskIO().execute {
            database.demandOfferDao().updateOfferSelection(offer.uid, select)
        }
    }

    fun confirmOptions() {
        CreateDemandWorker.createWorker(context)
    }

    @WorkerThread
    fun executeCreateDemand() {
        try {
            val list = database.demandOfferDao().getAllDirect()
            val callback = SagresNavigator.instance.createDemandOffer(list)

            val title = context.getString(R.string.demand_notification_title)
            // TODO this must be a string resource
            val content = when (callback.status) {
                Status.STARTED -> "Começou a requisição e nem terminou"
                Status.LOADING -> "Começou a carregar a requisição e não terminou"
                Status.INVALID_LOGIN -> "Login falhou..."
                Status.APPROVING -> "Erro desconhecido"
                Status.NETWORK_ERROR -> "Erro de internet, a mensagem é: ${callback.throwable?.message}"
                Status.RESPONSE_FAILED -> "Resposta falhou com código ${callback.code}"
                Status.SUCCESS -> "Demanda completa! Observe o Sagres para ter certeza [Beta]"
                Status.APPROVAL_ERROR -> "Algum erro ocorreu, provavelmente as matérias selecionadas não estão de acordo com as do servidor"
                Status.GRADES_FAILED -> "Esse erro não deveria acontecer, mas aconteceu"
                Status.UNKNOWN_FAILURE -> "Erro desconhecido, tudo que sei é: ${callback.message}"
                Status.COMPLETED -> "A demanda foi enviada, mas o Sagres não respondeu com sucesso, ele disse: ${callback.message}"
            }

            if (callback.status == Status.SUCCESS) {
                analytics.logEvent("demand_user_completed_last_flow", null)
            } else {
                analytics.logEvent("demand_user_failed_last_flow", bundleOf(
                    "status" to callback.status.name
                ))
            }

            NotificationCreator.showSimpleNotification(context, title, content)
        } catch (t: Throwable) {
            NotificationCreator.showSimpleNotification(context, context.getString(R.string.demand_notification_title), "Uma exceção muito louca ocorreu: ${t.message}")
            analytics.logEvent("demand_user_exception_at_worker", bundleOf(
                "message" to t.message
            ))
            Crashlytics.logException(t)
        }
    }
}