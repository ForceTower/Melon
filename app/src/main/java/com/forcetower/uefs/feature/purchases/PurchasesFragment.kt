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

package com.forcetower.uefs.feature.purchases

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetails
import com.forcetower.uefs.R
import com.forcetower.uefs.core.billing.SkuDetailsResult
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.BillingViewModel
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.forcetower.uefs.databinding.FragmentPurchasesBinding
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

class PurchasesFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var preferences: SharedPreferences

    private lateinit var viewModel: BillingViewModel
    private lateinit var binding: FragmentPurchasesBinding
    private lateinit var adapter: SkuDetailsAdapter

    private val list: MutableList<String> = mutableListOf()

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
        adapter = SkuDetailsAdapter(viewModel)
        viewModel.getSkus().observe(this, Observer {
            if (list != it) {
                list.clear()
                list.addAll(it)
                if (it.isNotEmpty()) {
                    getSkuDetails(it)
                }
            }
        })
        viewModel.selectSku.observe(this, EventObserver {
            purchaseFlow(it)
        })
        viewModel.purchaseUpdateEvent.observe(this, Observer {
            Timber.d("You just purchase ${it.size} items")
            it.forEach { purchase ->
                // TODO extract this method to somewhere else it will be usefull
                if (purchase.sku == "score_increase_common") {
                    Timber.d("Purchased score increase common")
                    val current = preferences.getInt("score_increase_value", 0)
                    val expires = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
                    preferences.edit()
                            .putInt("score_increase_value", current + 1)
                            .putLong("score_increase_expires", expires)
                            .apply()
                }
                viewModel.client.consumeToken(purchase.purchaseToken)
            }
        })
    }

    private fun getSkuDetails(list: List<String>) {
        viewModel.querySkuDetails(list).observe(this, Observer {
            processDetails(it)
        })
    }

    private fun processDetails(result: SkuDetailsResult) {
        if (result.responseCode == BillingClient.BillingResponse.OK) {
            val values = result.list
            Timber.d("Values: $values")
            adapter.submitList(values)
        } else {
            showSnack(getString(R.string.donation_service_response_error), true)
        }
    }

    private fun purchaseFlow(details: SkuDetails) {
        val params = BillingFlowParams.newBuilder()
                .setSkuDetails(details)
                .build()

        viewModel.client.billingClient.launchBillingFlow(requireActivity(), params)
    }
}