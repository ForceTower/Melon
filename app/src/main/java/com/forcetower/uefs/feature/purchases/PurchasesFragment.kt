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

package com.forcetower.uefs.feature.purchases

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.forcetower.uefs.R
import com.forcetower.uefs.core.billing.SkuDetailsResult
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.BillingViewModel
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentPurchasesBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

class PurchasesFragment : UFragment(), Injectable, PurchasesUpdatedListener, BillingClientStateListener {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var analytics: FirebaseAnalytics

    private lateinit var viewModel: BillingViewModel
    private lateinit var binding: FragmentPurchasesBinding
    private lateinit var skuAdapter: SkuDetailsAdapter
    private lateinit var billingClient: BillingClient

    private val list: MutableList<String> = mutableListOf()
    private val details: MutableList<SkuDetails> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            analytics.logEvent("purchases_screen", null)
        }

        billingClient = BillingClient.newBuilder(requireContext().applicationContext)
                .setListener(this)
                .build()

        if (!billingClient.isReady) {
            Timber.d("Starting Connection...")
            billingClient.startConnection(this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        return FragmentPurchasesBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            imageTop = "https://cdn.dribbble.com/users/1903950/screenshots/4225909/02_main_tr__1.gif"
            executePendingBindings()
            incToolbar.textToolbarTitle.text = getString(R.string.label_purchases)
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        skuAdapter = SkuDetailsAdapter(viewModel)
        binding.recyclerSku.apply {
            adapter = skuAdapter
        }
        viewModel.getSkus().observe(viewLifecycleOwner, Observer {
            if (list != it) {
                list.clear()
                list.addAll(it)
                if (it.isNotEmpty()) {
                    getSkuDetails(it)
                }
            }
        })
        viewModel.selectSku.observe(viewLifecycleOwner, EventObserver {
            purchaseFlow(it)
        })
    }

    private fun getSkuDetails(list: List<String>) {
        val params = SkuDetailsParams.newBuilder()
                .setSkusList(list)
                .setType(BillingClient.SkuType.INAPP)
                .build()
        billingClient.querySkuDetailsAsync(params) { code, details ->
            processDetails(SkuDetailsResult(code, details))
        }
    }

    private fun processDetails(result: SkuDetailsResult) {
        if (result.responseCode == BillingClient.BillingResponse.OK) {
            val values = result.list
            Timber.d("Values: $values")
            details.clear()
            if (values != null) details.addAll(values)
            skuAdapter.submitList(values)
        } else {
            showSnack(getString(R.string.donation_service_response_error), true)
            analytics.logEvent("purchases_failed", null)
        }
    }

    private fun purchaseFlow(details: SkuDetails) {
        val params = BillingFlowParams.newBuilder()
                .setSkuDetails(details)
                .build()

        billingClient.launchBillingFlow(requireActivity(), params)
    }

    private fun updatePurchases() {
        if (!billingClient.isReady) {
            Timber.d("BillingClient is not ready to query for existing purchases")
        }
        val result = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
        if (result == null) {
            Timber.d("Update purchase: Null purchase result")
        } else {
            if (result.purchasesList == null) {
                Timber.d("Update purchase: Null purchase list")
            } else {
                handlePurchases(result.purchasesList)
            }
        }
    }

    private fun handlePurchases(purchasesList: List<Purchase>?) {
        purchasesList ?: return
        purchasesList.forEach { purchase ->
            // TODO extract this method to somewhere else it will be useful
            if (purchase.sku == "score_increase_common") {
                Timber.d("Purchased score increase common")
                var currentIncrease = preferences.getFloat("score_increase_value", 0f)
                val currentExpire = preferences.getLong("score_increase_expires", -1)

                val now = Calendar.getInstance().timeInMillis
                if (currentExpire < now) currentIncrease = 0.0f

                val expires = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
                preferences.edit()
                        .putFloat("score_increase_value", currentIncrease + 0.2f)
                        .putLong("score_increase_expires", expires)
                        .apply()
            }
            billingClient.consumeAsync(purchase.purchaseToken) { code, token ->
                Timber.d("Attempt to consume $token finished with code $code")
            }

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                val item = details.find { it.sku == purchase.sku }

                if (item == null) {
                    showSnack(getString(R.string.you_bought_an_item))
                } else {
                    val value = item.title
                    val title = if (value.contains("(")) {
                        val index = value.lastIndexOf("(")
                        value.substring(0, index).trim()
                    } else {
                        item.title
                    }
                    showSnack(getString(R.string.you_bought_this_item, title))
                }
            }
        }
    }

    @SuppressLint("BinaryOperationInTimber")
    override fun onPurchasesUpdated(responseCode: Int, purchasesList: List<Purchase>?) {
        Timber.d("onPurchasesUpdated, response code: $responseCode")
        when (responseCode) {
            BillingClient.BillingResponse.OK -> {
                if (purchasesList == null) {
                    Timber.d("Purchase update: No purchases")
                    handlePurchases(null)
                } else {
                    handlePurchases(purchasesList)
                }
            }
            BillingClient.BillingResponse.USER_CANCELED -> {
                Timber.d("User canceled the purchase")
            }
            BillingClient.BillingResponse.ITEM_ALREADY_OWNED -> {
                Timber.d("The user already owns this item")
            }
            BillingClient.BillingResponse.DEVELOPER_ERROR -> {
                Timber.d("Developer error means that Google Play does not recognize the " +
                        "configuration. If you are just getting started, make sure you have " +
                        "configured the application correctly in the Google Play Console. " +
                        "The SKU product ID must match and the APK you are using must be " +
                        "signed with release keys.")
            }
            else -> {
                Timber.d("See error code in BillingClient.BillingResponse: $responseCode")
            }
        }
    }

    override fun onBillingSetupFinished(billingResponseCode: Int) {
        Timber.d("onBillingSetupFinished: $billingResponseCode")
        if (billingResponseCode == BillingClient.BillingResponse.OK) {
            updatePurchases()
        }
    }

    override fun onBillingServiceDisconnected() {
        Timber.d("onBillingServiceDisconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (billingClient.isReady) {
            Timber.d("Closing connection")
            billingClient.endConnection()
        }
    }
}