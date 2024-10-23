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

package com.forcetower.uefs.feature.evaluation.discipline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.databinding.FragmentEvaluationDisciplineBinding
import com.forcetower.uefs.domain.model.paradox.DisciplineCombinedData
import com.forcetower.uefs.feature.evaluation.EvaluationState
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisciplineEvaluationFragment : UFragment() {
    private val viewModel: EvaluationViewModel by viewModels()
    private lateinit var binding: FragmentEvaluationDisciplineBinding
    private lateinit var elements: EvaluationElementsAdapter
    private val args: DisciplineEvaluationFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        elements = EvaluationElementsAdapter(viewModel)
        return FragmentEvaluationDisciplineBinding.inflate(inflater, container, false).also {
            binding = it
            binding.btnEvaluate.hide()
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.fetchDiscipline(args.id)
        viewModel.discipline.observe(viewLifecycleOwner) {
            handleData(it)
        }
        binding.itemsRecycler.apply {
            adapter = elements
            itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
        }
        viewModel.teacherIntSelect.observe(
            viewLifecycleOwner,
            EventObserver {
                val directions = DisciplineEvaluationFragmentDirections.actionDisciplineToTeacher(it.id)
                findNavController().navigate(directions)
            }
        )

        viewModel.state.observe(viewLifecycleOwner, ::handleState)
        binding.btnEvaluate.isVisible = false
    }

    private fun handleData(data: DisciplineCombinedData) {
        val evaluation = DisciplineEvaluation(
            data.ref.discipline,
            data.ref.departmentName,
            data.ref.studentCount,
            data.semesters.map {
                SemesterMean(it.id, it.name, it.mean)
            },
            data.teachers.map {
                TeacherInt(it.id, it.name, it.lastSeen, it.mean)
            }.sortedBy { it.name }
        )
        elements.discipline = evaluation
    }

    private fun handleState(state: EvaluationState) {
        when {
            state.failed -> {
                binding.itemsRecycler.visibility = GONE
                binding.loadingGroup.visibility = GONE
                binding.failedGroup.visibility = VISIBLE
            }
            state.loading -> {
                binding.itemsRecycler.visibility = GONE
                binding.loadingGroup.visibility = VISIBLE
                binding.failedGroup.visibility = GONE
            }
            else -> {
                binding.itemsRecycler.visibility = VISIBLE
                binding.loadingGroup.visibility = GONE
                binding.failedGroup.visibility = GONE
            }
        }
    }
}

data class DisciplineEvaluation(
    val name: String,
    val department: String,
    val amount: Int,
    val grades: List<SemesterMean>,
    val teachers: List<TeacherInt> = emptyList()
)

data class SemesterMean(
    val id: Long,
    val name: String,
    val mean: Double
)

data class TeacherInt(
    val id: String,
    val name: String,
    val lastSeen: String,
    val mean: Double
)
