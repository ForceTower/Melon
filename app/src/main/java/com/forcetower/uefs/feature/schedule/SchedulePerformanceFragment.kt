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

package com.forcetower.uefs.feature.schedule

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.forcetower.core.injection.Injectable
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentSchedulePerformanceBinding
import com.forcetower.uefs.feature.captcha.CaptchaResolverFragment
import com.forcetower.uefs.feature.profile.ProfileViewModel
import com.forcetower.uefs.feature.shared.UFragment
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max

class SchedulePerformanceFragment : UFragment(), Injectable {
    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var factory: UViewModelFactory
    private val viewModel by viewModels<ScheduleViewModel> { factory }
    private val profileViewModel by activityViewModels<ProfileViewModel> { factory }

    private lateinit var binding: FragmentSchedulePerformanceBinding
    private var showEmptyDays = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentSchedulePerformanceBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            actions = viewModel
            lifecycleOwner = this@SchedulePerformanceFragment
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showEmptyDays = preferences.getBoolean("stg_show_empty_day_schedule", false)
        val manager = GridLayoutManager(requireContext(), 6)
        val adapter = ScheduleBlockPerformanceAdapter(viewModel, showEmptyDays, requireContext())
        val adapterLine = ScheduleLinePerformanceAdapter(viewModel)
        binding.recyclerSchedule.layoutManager = manager
        binding.recyclerSchedule.adapter = adapter
        binding.recyclerScheduleLine.adapter = adapterLine

        viewModel.hasSchedule.observe(viewLifecycleOwner, Observer { binding.empty = !it })
        viewModel.schedule.observe(viewLifecycleOwner, Observer {
            manager.spanCount = max(it.keys.size, if (showEmptyDays) 6 else 0)
            adapter.elements = it

            adapterLine.elements = it
        })

        viewModel.onRefresh.observe(viewLifecycleOwner, EventObserver {
            onRefresh()
        })

        if (TimeUtils.eventHasEnded()) {
            binding.btnConferenceSchedule.visibility = View.GONE
        } else {
            profileViewModel.commonProfile.observe(viewLifecycleOwner, Observer {
                val course = it?.course ?: 1L
                if (course == 1L && preferences.isStudentFromUEFS()) {
                    binding.btnConferenceSchedule.visibility = View.VISIBLE
                }
            })
        }
    }

    private fun onRefresh() {
        if (!preferences.isStudentFromUEFS()) {
            viewModel.doRefreshData(null)
            return
        }

        val fragment = CaptchaResolverFragment()
        fragment.setCallback(object : CaptchaResolverFragment.CaptchaResolvedCallback {
            override fun onCaptchaResolved(token: String) {
                Timber.d("Token received $token")
                viewModel.doRefreshData(token)
            }
        })

        fragment.show(childFragmentManager, "captcha_resolver")
    }
}