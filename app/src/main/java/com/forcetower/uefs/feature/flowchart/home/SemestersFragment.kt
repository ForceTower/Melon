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
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI
import com.forcetower.uefs.core.util.toJson
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentFlowchartSemestersBinding
import com.forcetower.uefs.feature.flowchart.FlowchartViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import timber.log.Timber
import javax.inject.Inject

class SemestersFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var binding: FragmentFlowchartSemestersBinding
    private lateinit var viewModel: FlowchartViewModel
    private lateinit var adapter: SemesterAdapter

    init {
        displayName = "Semestres"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
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