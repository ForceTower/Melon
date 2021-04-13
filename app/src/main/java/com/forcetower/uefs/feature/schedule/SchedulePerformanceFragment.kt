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
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.sagres.Constants
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import com.forcetower.uefs.databinding.FragmentSchedulePerformanceBinding
import com.forcetower.uefs.feature.captcha.CaptchaResolverFragment
import com.forcetower.uefs.feature.profile.ProfileViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class SchedulePerformanceFragment : UFragment() {
    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var remoteConfig: FirebaseRemoteConfig
    private val viewModel by activityViewModels<ScheduleViewModel>()
    private val profileViewModel by activityViewModels<ProfileViewModel>()

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
        val manager = WrappedGridLayoutManager(requireContext(), 6, showEmptyDays)
        val adapter = ScheduleBlockPerformanceAdapter(viewModel, showEmptyDays, requireContext())
        val adapterLine = ScheduleLinePerformanceAdapter(viewModel)
        binding.recyclerSchedule.layoutManager = manager
        binding.recyclerSchedule.adapter = adapter
        binding.recyclerScheduleLine.adapter = adapterLine

        viewModel.hasSchedule.observe(viewLifecycleOwner, { binding.empty = !it })
        viewModel.schedule.observe(
            viewLifecycleOwner,
            {
                val gridMax = if (it.keys.contains(7)) 7 else 6
                manager.spanCount = max(it.keys.size, if (showEmptyDays) gridMax else 0)
                adapter.elements = it
            }
        )
        viewModel.scheduleLine.observe(viewLifecycleOwner) {
            adapterLine.elements = it
        }

        viewModel.onRefresh.observe(
            viewLifecycleOwner,
            EventObserver {
                onRefresh()
            }
        )

        if (TimeUtils.eventHasEnded()) {
            binding.btnConferenceSchedule.visibility = View.GONE
        } else {
            profileViewModel.commonProfile.observe(
                viewLifecycleOwner,
                {
                    val course = it?.course ?: 1L
                    if (course == 1L && preferences.isStudentFromUEFS()) {
                        binding.btnConferenceSchedule.visibility = View.VISIBLE
                    }
                }
            )
        }
    }

    private fun onRefresh() {
        Timber.d("Refresh requested.... ${Constants.getParameter("REQUIRES_CAPTCHA")}")
        val snowpiercer = remoteConfig.getBoolean("feature_flag_use_snowpiercer") && preferences.isStudentFromUEFS()
        if (Constants.getParameter("REQUIRES_CAPTCHA") != "true" || snowpiercer) {
            viewModel.doRefreshData(null, snowpiercer)
            return
        }

        val fragment = CaptchaResolverFragment()
        fragment.setCallback(
            object : CaptchaResolverFragment.CaptchaResolvedCallback {
                override fun onCaptchaResolved(token: String) {
                    Timber.d("Token received $token")
                    viewModel.doRefreshData(token, false)
                }
            }
        )

        fragment.show(childFragmentManager, "captcha_resolver")
    }
}
