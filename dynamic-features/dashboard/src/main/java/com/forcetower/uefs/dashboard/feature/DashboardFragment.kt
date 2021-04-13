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

import android.app.ActivityOptions
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.core.injection.dependencies.DashboardModuleDependencies
import com.forcetower.uefs.dashboard.R
import com.forcetower.uefs.dashboard.core.injection.DaggerDashboardComponent
import com.forcetower.uefs.dashboard.databinding.FragmentDashboardBinding
import com.forcetower.uefs.feature.home.HomeViewModel
import com.forcetower.uefs.feature.profile.ProfileActivity
import com.forcetower.uefs.feature.shared.UFragment
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.splitcompat.SplitCompat
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber
import javax.inject.Inject

@Keep
class DashboardFragment : UFragment() {
    @Inject lateinit var factory: ViewModelProvider.Factory

    private lateinit var binding: FragmentDashboardBinding
    private val viewModel: DashboardViewModel by activityViewModels { factory }
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        SplitCompat.install(context)
        DaggerDashboardComponent.builder()
            .context(context)
            .dependencies(
                EntryPointAccessors.fromActivity(
                    requireActivity(),
                    DashboardModuleDependencies::class.java
                )
            )
            .build()
            .inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentDashboardBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dashAdapter = DashboardAdapter(viewModel, viewLifecycleOwner)
        binding.recyclerElements.run {
            adapter = dashAdapter
            itemAnimator?.run {
                addDuration = 0L
                changeDuration = 0L
                moveDuration = 0L
                removeDuration = 0L
            }
        }

        homeViewModel.inAppUpdateStatus.observe(
            viewLifecycleOwner,
            Observer {
                dashAdapter.updatingApp = it == InstallStatus.DOWNLOADING
            }
        )
        viewModel.currentClass.observe(viewLifecycleOwner, Observer { dashAdapter.nextClass = it })
        viewModel.lastMessage.observe(viewLifecycleOwner, Observer { dashAdapter.lastMessage = it })
        viewModel.student.observe(viewLifecycleOwner, Observer { dashAdapter.student = it })
        viewModel.affinity.observe(viewLifecycleOwner, Observer { dashAdapter.affinityList = it })
        viewModel.account.observe(
            viewLifecycleOwner,
            Observer {
                dashAdapter.currentAccount = it
            }
        )
        viewModel.onMoveToSchedule.observe(viewLifecycleOwner, EventObserver { homeViewModel.onMoveToSchedule() })
        viewModel.profileClick.observe(
            viewLifecycleOwner,
            EventObserver {
                val accountId = it.first
                val profileId = it.second
                val intent = ProfileActivity.startIntent(requireContext(), profileId, accountId)

                val shared = findStudentHeadshot(binding.recyclerElements)
                val option = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), shared, "student_headshot_transition")
                startActivity(intent, option.toBundle())
            }
        )
    }

    private fun findStudentHeadshot(entities: ViewGroup): View {
        entities.forEach {
            if (it.getTag(R.id.tag_header_id) == "header") {
                return it.findViewById(R.id.profile_image)
            }
        }
        Timber.e("Could not find view")
        return entities
    }
}
