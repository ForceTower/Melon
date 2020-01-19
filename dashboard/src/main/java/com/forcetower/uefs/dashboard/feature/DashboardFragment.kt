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

package com.forcetower.uefs.dashboard.feature

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.forcetower.core.base.BaseViewModelFactory
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.dashboard.core.injection.DaggerDashboardComponent
import com.forcetower.uefs.dashboard.databinding.FragmentDashboardBinding
import com.forcetower.uefs.feature.home.HomeViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.splitcompat.SplitCompat
import javax.inject.Inject

@Keep
class DashboardFragment : UFragment() {
    @Inject
    lateinit var factory: BaseViewModelFactory
    @Inject
    lateinit var appFactory: UViewModelFactory

    private lateinit var binding: FragmentDashboardBinding
    private val viewModel: DashboardViewModel by activityViewModels { factory }
    private val homeViewModel: HomeViewModel by activityViewModels { appFactory }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        SplitCompat.install(context)
        val component = (context.applicationContext as UApplication).component
        DaggerDashboardComponent.builder().appComponent(component).build().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentDashboardBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dashAdapter = DashboardAdapter(viewModel, this)
        binding.recyclerElements.run {
            adapter = dashAdapter
        }

        homeViewModel.inAppUpdateStatus.observe(this, Observer {
            dashAdapter.updatingApp = it == InstallStatus.DOWNLOADING
        })
        viewModel.currentClass.observe(this, Observer { dashAdapter.nextClass = it })
        viewModel.lastMessage.observe(this, Observer { dashAdapter.lastMessage = it })
    }
}