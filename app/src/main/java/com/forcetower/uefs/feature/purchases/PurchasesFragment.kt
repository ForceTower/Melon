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

package com.forcetower.uefs.feature.purchases

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.core.billing.SkuDetailsResult
import com.forcetower.uefs.core.vm.BillingViewModel
import com.forcetower.uefs.databinding.FragmentPurchasesBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PurchasesFragment : UFragment() {
    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var analytics: FirebaseAnalytics

    private val viewModel: BillingViewModel by activityViewModels()
    private lateinit var binding: FragmentPurchasesBinding
    private lateinit var skuAdapter: SkuDetailsAdapter

    private val details: MutableList<SkuDetails> = mutableListOf()

    private var currentUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            analytics.logEvent("purchases_screen", null)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
        viewModel.subscriptions.observe(
            viewLifecycleOwner,
            Observer {
                processDetails(it)
            }
        )
        viewModel.selectSku.observe(
            viewLifecycleOwner,
            EventObserver {
                purchaseFlow(it)
            }
        )
        viewModel.currentUsername.observe(
            viewLifecycleOwner,
            Observer {
                currentUsername = it
            }
        )
    }

    private fun processDetails(result: SkuDetailsResult) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            val values = result.list
            details.clear()
            if (values != null) details.addAll(values)
            skuAdapter.submitList(values)
        } else {
            showSnack("${getString(R.string.donation_service_response_error)} ${result.responseCode}", Snackbar.LENGTH_LONG)
            analytics.logEvent("purchases_failed", null)
        }
    }

    private fun purchaseFlow(details: SkuDetails) {
        val username = currentUsername
        if (username != null) {
            viewModel.launchBillingFlow(requireActivity(), details, username)
        }
    }
}
