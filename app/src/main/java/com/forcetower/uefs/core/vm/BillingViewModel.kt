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

package com.forcetower.uefs.core.vm

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.R
import com.forcetower.uefs.core.billing.SkuDetailsResult
import com.forcetower.uefs.core.storage.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class BillingViewModel @Inject constructor(
    context: Context,
    private val repository: BillingRepository
) : ViewModel(), PurchasesUpdatedListener, BillingClientStateListener {
    private val _selectSku = MutableLiveData<Event<SkuDetails>>()
    val selectSku: LiveData<Event<SkuDetails>>
        get() = _selectSku

    private val _snack = MutableLiveData<Event<Int>>()
    val snack: LiveData<Event<Int>>
        get() = _snack

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .enablePendingPurchases()
        .setListener(this)
        .build()

    init {
        if (!billingClient.isReady) {
            billingClient.startConnection(this)
        }
    }

    private suspend fun queryGoldMonkey(): Boolean {
        try {
            val purchases = billingClient.suspendQueryPurchases(BillingClient.SkuType.SUBS)
            if (purchases.isEmpty()) {
                repository.cancelSubscriptions()
            } else {
                repository.handlePurchases(purchases)
            }
        } catch (error: Throwable) {
            Timber.i(error, "Error during purchase update")
        }
        return repository.isGoldMonkey()
    }

    val currentUsername: LiveData<String?>
        get() = repository.getUsername()

    fun getSkus() = repository.getManagedSkus()

    val subscriptions = getSkus().switchMap { skuList -> getSubscriptions(skuList) }
    val inAppProducts = getSkus().switchMap { skuList -> getInAppProducts(skuList) }

    fun selectSku(skuDetails: SkuDetails?) {
        skuDetails ?: return
        _selectSku.value = Event(skuDetails)
    }

    private fun getSubscriptions(list: List<String>): LiveData<SkuDetailsResult> {
        val subscriptions = MutableLiveData<SkuDetailsResult>()
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(list)
            .setType(BillingClient.SkuType.SUBS)
            .build()

        billingClient.querySkuDetailsAsync(params) { response, details ->
            subscriptions.value = SkuDetailsResult(response.responseCode, details)
        }
        return subscriptions
    }

    private fun getInAppProducts(list: List<String>): LiveData<SkuDetailsResult> {
        val inAppProducts = MutableLiveData<SkuDetailsResult>()
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(list)
            .setType(BillingClient.SkuType.INAPP)
            .build()

        billingClient.querySkuDetailsAsync(params) { response, details ->
            inAppProducts.value = SkuDetailsResult(response.responseCode, details)
        }
        return inAppProducts
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    handlePurchases(purchases)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.d("User canceled purchase")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _snack.postValue(Event(R.string.purchase_item_already_owned))
            }
            else -> {
                _snack.postValue(Event(R.string.purchase_update_error))
                Timber.e("Error purchase: ${result.responseCode}")
            }
        }
    }

    fun launchBillingFlow(activity: Activity, details: SkuDetails, username: String) {
        val params = BillingFlowParams.newBuilder()
            .setSkuDetails(details)
            .setObfuscatedAccountId(username)
            .build()

        billingClient.launchBillingFlow(activity, params)
    }

    private suspend fun updatePurchases() {
        val result = try {
            billingClient.suspendQueryPurchases(BillingClient.SkuType.INAPP)
        } catch (error: Throwable) {
            null
        }

        if (result == null) {
            Timber.d("Update purchase: Null purchase list")
        } else {
            handlePurchases(result)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        repository.handlePurchases(purchases)
        purchases.forEach {
            if (!it.isAutoRenewing) {
                consume(it)
            } else {
                _snack.postValue(Event(R.string.purchase_subscription_started))
            }
        }
    }

    override fun onBillingServiceDisconnected() = Unit

    override fun onBillingSetupFinished(result: BillingResult) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            viewModelScope.launch {
                updatePurchases()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingClient.endConnection()
    }

    private fun consume(purchase: Purchase) {
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.consumeAsync(params) { response, token ->
            Timber.d("Attempt to consume $token finished with code ${response.responseCode}")
            if (response.responseCode == BillingClient.BillingResponseCode.OK) {
                _snack.postValue(Event(R.string.purchase_you_bought_a_consumable_item))
            } else {
                Timber.e("Failed to consume ${purchase.skus}")
            }
        }
    }

    private suspend fun BillingClient.suspendQueryPurchases(type: String) = suspendCancellableCoroutine<List<Purchase>> { continuation ->
        queryPurchasesAsync(type) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                continuation.resume(purchases)
            } else {
                continuation.resumeWithException(IllegalStateException("Response code was ${result.responseCode}"))
            }
        }
    }
}
