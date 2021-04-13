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

package com.forcetower.uefs.feature.flowchart.semester

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI
import com.forcetower.uefs.databinding.FragmentFlowchartSemesterBinding
import com.forcetower.uefs.feature.flowchart.FlowchartViewModel
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SemesterFragment : UFragment() {
    private val viewModel: FlowchartViewModel by viewModels()
    private val args by navArgs<SemesterFragmentArgs>()
    private lateinit var binding: FragmentFlowchartSemesterBinding
    private lateinit var adapter: DisciplinesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        adapter = DisciplinesAdapter(viewModel)
        return FragmentFlowchartSemesterBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            recyclerDisciplines.adapter = adapter
            up.setOnClickListener { backToRoot() }
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getDisciplinesUI(args.semesterId).observe(viewLifecycleOwner, Observer { onDisciplinesReceived(it) })
        viewModel.getSemesterUI(args.semesterId).observe(viewLifecycleOwner, Observer { onSemesterReceived(it) })
        viewModel.onDisciplineSelect.observe(viewLifecycleOwner, EventObserver { onDisciplineSelected(it) })
    }

    private fun onSemesterReceived(value: FlowchartSemesterUI) {
        binding.semester = value
        binding.executePendingBindings()
    }

    private fun onDisciplinesReceived(list: List<FlowchartDisciplineUI>) {
        adapter.submitList(list)
    }

    private fun backToRoot() {
        findNavController().navigateUp()
    }

    private fun onDisciplineSelected(discipline: FlowchartDisciplineUI) {
        val direction = SemesterFragmentDirections.actionSemesterToDiscipline(discipline.id)
        findNavController().navigate(direction)
    }
}
