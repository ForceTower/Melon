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

package com.forcetower.uefs.feature.schedule

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.storage.database.accessors.LocationWithGroup
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentScheduleBinding
import com.forcetower.uefs.feature.profile.ProfileViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.forcetower.uefs.feature.siecomp.SIECOMPActivity
import javax.inject.Inject

class ScheduleFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var preferences: SharedPreferences

    private lateinit var viewModel: ScheduleViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var binding: FragmentScheduleBinding

    private var showHidden: Boolean = false

    private val linePool = RecyclerView.RecycledViewPool()
    private val lineAdapter by lazy { ScheduleLineAdapter(this, viewModel, linePool) }
    private val blockPool = RecyclerView.RecycledViewPool()
    private lateinit var blockAdapter: ScheduleBlockAdapter

    init {
        linePool.setMaxRecycledViews(1, 4)
        linePool.setMaxRecycledViews(2, 5)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        profileViewModel = provideActivityViewModel(factory)
        showHidden = preferences.getBoolean("stg_show_empty_day_schedule", false)
        blockAdapter = ScheduleBlockAdapter(blockPool, this, viewModel, requireContext(), showHidden)

        return FragmentScheduleBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            actions = this@ScheduleFragment.viewModel
            lifecycleOwner = this@ScheduleFragment
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            recyclerScheduleLine.apply {
                setRecycledViewPool(linePool)
                adapter = lineAdapter
                itemAnimator?.run {
                    addDuration = 120L
                    moveDuration = 120L
                    changeDuration = 120L
                    removeDuration = 100L
                }
            }

            recyclerScheduleBlocks.apply {
                setRecycledViewPool(blockPool)
                adapter = blockAdapter
                itemAnimator?.run {
                    addDuration = 120L
                    moveDuration = 120L
                    changeDuration = 120L
                    removeDuration = 100L
                }
            }

            if (VersionUtils.isMarshmallow()) {
                layoutData.setOnScrollChangeListener { _, _, _, _, _ ->
                    if (layoutData.scrollY > 0)
                        appBar.elevation = getPixelsFromDp(requireContext(), 4)
                    else
                        appBar.elevation = 0f
                }
            }
        }

        binding.btnSiecompSchedule.setOnClickListener {
            val intent = Intent(requireContext(), SIECOMPActivity::class.java)
            startActivity(intent)
        }

        viewModel.scheduleSrc.observe(this, Observer { populateInterface(it) })
        if (TimeUtils.eventHasEnded()) {
            binding.btnSiecompSchedule.visibility = GONE
        } else {
            // profileViewModel.getMeProfile().observe(this, Observer {
                binding.btnSiecompSchedule.show()
                binding.btnSiecompSchedule.extend()
                binding.btnSiecompSchedule.visibility = VISIBLE
            // })
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.appBar.elevation = 0f
    }

    private fun populateInterface(locations: List<LocationWithGroup>) {
        binding.empty = locations.isEmpty()
        binding.executePendingBindings()
        val sorted = locations.toMutableList()
        sorted.sort()
        lineAdapter.adaptList(sorted)
        blockAdapter.adaptList(locations)
    }
}