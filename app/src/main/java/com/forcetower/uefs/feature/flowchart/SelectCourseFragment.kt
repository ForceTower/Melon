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

package com.forcetower.uefs.feature.flowchart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.databinding.FragmentFlowchartSelectCourseBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SelectCourseFragment : UFragment() {
    private val viewModel: FlowchartViewModel by viewModels()
    private lateinit var binding: FragmentFlowchartSelectCourseBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentFlowchartSelectCourseBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val coursesAdapter = CourseAdapter(viewModel)
        binding.recyclerCourses.apply {
            adapter = coursesAdapter
        }

        viewModel.getFlowcharts().observe(
            viewLifecycleOwner,
            Observer {
                Timber.d("Resource data ${it.data}")
                if (it.data != null) {
                    coursesAdapter.submitList(it.data)
                }
            }
        )

        viewModel.onFlowchartSelect.observe(
            viewLifecycleOwner,
            EventObserver {
                val direction = SelectCourseFragmentDirections.actionSelectToStart(it.courseId)
                findNavController().navigate(direction)
            }
        )
    }
}
