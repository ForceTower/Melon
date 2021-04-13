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

package com.forcetower.uefs.feature.flowchart.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI
import com.forcetower.uefs.core.util.toJson
import com.forcetower.uefs.databinding.FragmentFlowchartSemestersBinding
import com.forcetower.uefs.feature.flowchart.FlowchartViewModel
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SemestersFragment : UFragment() {
    private lateinit var binding: FragmentFlowchartSemestersBinding
    private val viewModel: FlowchartViewModel by viewModels()
    private lateinit var adapter: SemesterAdapter

    init {
        displayName = "Semestres"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        adapter = SemesterAdapter(viewModel)
        viewModel.setCourse(arguments?.getLong("course_id") ?: 0)
        return FragmentFlowchartSemestersBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            flowSemesterRecycler.adapter = adapter
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.flowchart.observe(viewLifecycleOwner, Observer { onSemestersReceived(it) })
        viewModel.onSemesterSelect.observe(viewLifecycleOwner, EventObserver { onSemesterSelected(it) })
    }

    private fun onSemestersReceived(values: List<FlowchartSemesterUI>) {
        adapter.submitList(values)
    }

    private fun onSemesterSelected(semester: FlowchartSemesterUI) {
        Timber.d("Semester selected ${semester.toJson()}")
        val direction = FlowchartFragmentDirections.actionHomeToSemester(semester.id)
        findNavController().navigate(direction)
    }

    companion object {
        fun newInstance(courseId: Long) = SemestersFragment().apply {
            arguments = bundleOf("course_id" to courseId)
        }
    }
}
