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

package com.forcetower.uefs.feature.disciplines.disciplinedetail.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.forcetower.uefs.databinding.FragmentDisciplineOverviewBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.disciplines.disciplinedetail.DisciplineDetailsActivity.Companion.CLASS_GROUP_ID
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OverviewFragment : UFragment() {
    private lateinit var binding: FragmentDisciplineOverviewBinding
    private val viewModel: DisciplineViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentDisciplineOverviewBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val overviewAdapter = OverviewAdapter(this, viewModel)
        binding.recyclerOverview.apply {
            adapter = overviewAdapter
            itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
        }

        viewModel.clazz.observe(viewLifecycleOwner, Observer { overviewAdapter.currentClazz = it })
        viewModel.group.observe(viewLifecycleOwner, Observer { overviewAdapter.currentGroup = it })
        viewModel.schedule.observe(viewLifecycleOwner, Observer { overviewAdapter.currentSchedule = it })
    }

    companion object {
        fun newInstance(classId: Long): OverviewFragment {
            return OverviewFragment().apply {
                arguments = bundleOf(CLASS_GROUP_ID to classId)
            }
        }
    }
}
