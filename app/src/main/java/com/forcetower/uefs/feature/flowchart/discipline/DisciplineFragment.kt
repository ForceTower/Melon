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

package com.forcetower.uefs.feature.flowchart.discipline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.core.model.unes.FlowchartRequirementUI
import com.forcetower.uefs.core.model.unes.FlowchartSemester
import com.forcetower.uefs.databinding.FragmentFlowchartDisciplineDetailsBinding
import com.forcetower.uefs.feature.flowchart.FlowchartViewModel
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class DisciplineFragment : UFragment() {
    private val viewModel: FlowchartViewModel by viewModels()
    private val args: DisciplineFragmentArgs by navArgs()
    private lateinit var binding: FragmentFlowchartDisciplineDetailsBinding
    private lateinit var adapter: DisciplineDetailsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        adapter = DisciplineDetailsAdapter(viewModel)
        return FragmentFlowchartDisciplineDetailsBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            up.setOnClickListener { onUpPressed() }
            disciplineDetailsRecycler.adapter = adapter
        }.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.getDisciplineUi(args.disciplineId).observe(this, Observer { onReceiveDiscipline(it) })
        viewModel.getSemesterName(args.disciplineId).observe(this, Observer { onReceiveSemesterName(it) })
        viewModel.getRequirementsUI(args.disciplineId).observe(this, Observer { onReceiveRequirements(it) })
        viewModel.onRequirementSelect.observe(this, EventObserver { onRequirementSelected(it) })
    }

    private fun onReceiveRequirements(requirements: List<FlowchartRequirementUI>) {
        Timber.d("New fragments size received")
        adapter.currentList = requirements
    }

    private fun onReceiveSemesterName(semester: FlowchartSemester?) {
        adapter.semesterValue = semester?.name
    }

    private fun onReceiveDiscipline(discipline: FlowchartDisciplineUI?) {
        adapter.discipline = discipline
    }

    private fun onRequirementSelected(requirement: FlowchartRequirementUI) {
        if (requirement.requiredDisciplineId != null) {
            val id = if (requirement.type == getString(R.string.flowchart_recursive_unlock))
                requirement.disciplineId
            else
                requirement.requiredDisciplineId
            val direction = DisciplineFragmentDirections.actionDisciplineSelf(id)
            findNavController().navigate(direction)
        }
    }

    private fun onUpPressed() {
        findNavController().navigateUp()
    }
}
