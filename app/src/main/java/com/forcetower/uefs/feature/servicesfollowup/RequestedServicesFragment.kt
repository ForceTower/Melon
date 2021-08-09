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

package com.forcetower.uefs.feature.servicesfollowup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.forcetower.uefs.databinding.ContentServicesFollowupBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RequestedServicesFragment : UFragment() {
    private val viewModel: ServicesFollowUpViewModel by activityViewModels()
    private lateinit var binding: ContentServicesFollowupBinding

    private val adapter by lazy { ServicesFollowUpAdapter() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ContentServicesFollowupBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerServices.run {
            adapter = this@RequestedServicesFragment.adapter
        }

        val filter = arguments?.getString(FILTER_TYPE)
        viewModel.getRequestedServices(filter).observe(
            viewLifecycleOwner,
            {
                binding.empty = it.isEmpty()
                binding.executePendingBindings()
                adapter.submitList(it)
            }
        )
    }

    companion object {
        private const val FILTER_TYPE = "requested_services_filter"
        fun newInstance(filter: String? = null): RequestedServicesFragment {
            return RequestedServicesFragment().apply {
                arguments = bundleOf(FILTER_TYPE to filter)
            }
        }
    }
}
