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

package com.forcetower.uefs.feature.servicesfollowup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.forcetower.uefs.databinding.FragmentServicesFollowupBinding
import com.forcetower.uefs.feature.shared.NamedFragmentAdapter
import javax.inject.Inject

class ServicesFollowUpFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    lateinit var viewModel: ServicesFollowUpViewModel
    lateinit var binding: FragmentServicesFollowupBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        return FragmentServicesFollowupBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            viewModel = this@ServicesFollowUpFragment.viewModel
            lifecycleOwner = this@ServicesFollowUpFragment
            executePendingBindings()
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pager = binding.pagerServices
        val tabs = binding.tabLayout

        pager.adapter = NamedFragmentAdapter(childFragmentManager, listOf(
            getString(R.string.service_requests_incomplete) to RequestedServicesFragment.newInstance("incomplete"),
            getString(R.string.service_requests_completed) to RequestedServicesFragment.newInstance("complete"),
            getString(R.string.service_requests_all) to RequestedServicesFragment.newInstance()
        ))
        tabs.setupWithViewPager(pager)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.pendingServices.observe(this, EventObserver {
            if (it.status == Status.ERROR) {
                showSnack(getString(R.string.service_requests_load_failed))
            }
        })
    }
}