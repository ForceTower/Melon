/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.storage.repository

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MediatorLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SagresDemandOffer
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.work.demand.CreateDemandWorker
import com.forcetower.uefs.service.NotificationCreator
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.BackpressureStrategy
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

    fun loadDemand(): LiveData<Resource<List<SagresDemandOffer>>> {
        val result = MediatorLiveData<Resource<List<SagresDemandOffer>>>()

        result.value = Resource.loading(null)
        val local = database.demandOfferDao().getAll()
        val network = LiveDataReactiveStreams.fromPublisher(
            SagresNavigator.instance.aLoadDemandOffers().toFlowable(BackpressureStrategy.LATEST)
        )

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

    fun updateOfferSelection(offer: SagresDemandOffer, select: Boolean) {
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
                analytics.logEvent(
                    "demand_user_failed_last_flow",
                    bundleOf(
                        "status" to callback.status.name
                    )
                )
            }

            NotificationCreator.showSimpleNotification(context, title, content)
        } catch (t: Throwable) {
            NotificationCreator.showSimpleNotification(context, context.getString(R.string.demand_notification_title), "Uma exceção muito louca ocorreu: ${t.message}")
            analytics.logEvent(
                "demand_user_exception_at_worker",
                bundleOf(
                    "message" to t.message
                )
            )
            Timber.e(t)
        }
    }
}
