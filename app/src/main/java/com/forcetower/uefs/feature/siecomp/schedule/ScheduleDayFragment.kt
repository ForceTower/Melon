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

package com.forcetower.uefs.feature.siecomp.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.storage.eventdatabase.accessors.SessionWithData
import com.forcetower.uefs.core.util.siecomp.TimeUtils.SIECOMP_TIMEZONE
import com.forcetower.uefs.databinding.FragmentEventScheduleDayBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.clearDecorations
import com.forcetower.uefs.feature.siecomp.SIECOMPEventViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScheduleDayFragment : UFragment() {
    private lateinit var binding: FragmentEventScheduleDayBinding
    private lateinit var adapter: ScheduleDayAdapter
    private val viewModel: SIECOMPEventViewModel by activityViewModels()
    private val tagViewPool = RecyclerView.RecycledViewPool()
    private val sessionViewPool = RecyclerView.RecycledViewPool()

    init {
        tagViewPool.setMaxRecycledViews(0, 15)
        sessionViewPool.setMaxRecycledViews(0, 10)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEventScheduleDayBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@ScheduleDayFragment
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ScheduleDayAdapter(viewModel, tagViewPool, SIECOMP_TIMEZONE)
        binding.recyclerDaySchedule.apply {
            setRecycledViewPool(sessionViewPool)
            adapter = this@ScheduleDayFragment.adapter
            (layoutManager as LinearLayoutManager).recycleChildrenOnDetach = true
            (itemAnimator as DefaultItemAnimator).run {
                supportsChangeAnimations = false
                addDuration = 160L
                moveDuration = 160L
                changeDuration = 160L
                removeDuration = 120L
            }
        }

        viewModel.getSessionsFromDayLocal(requireArguments().getInt(ARG_EVENT_DAY)).observe(
            viewLifecycleOwner,
            Observer {
                it ?: return@Observer
                populateInterface(it)
            }
        )
    }

    private fun populateInterface(data: List<SessionWithData>) {
        adapter.submitList(data)
        binding.recyclerDaySchedule.run {
            doOnNextLayout {
                clearDecorations()
                if (data.isNotEmpty()) {
                    addItemDecoration(
                        ScheduleItemHeaderDecoration(
                            it.context,
                            data,
                            SIECOMP_TIMEZONE
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val ARG_EVENT_DAY = "arg.EVENT_DAY"

        fun newInstance(day: Int): ScheduleDayFragment {
            val args = bundleOf(ARG_EVENT_DAY to day)
            return ScheduleDayFragment().apply { arguments = args }
        }
    }
}
