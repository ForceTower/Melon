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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.core.model.service.EvaluationDiscipline
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.databinding.FragmentEvaluationDisciplineBinding
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

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
        viewModel.getDiscipline(args.department, args.code).observe(viewLifecycleOwner, { handleData(it) })
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
                val directions = DisciplineEvaluationFragmentDirections.actionDisciplineToTeacher(it.id, null)
                findNavController().navigate(directions)
            }
        )
        binding.btnEvaluate.setOnClickListener {
            val directions = DisciplineEvaluationFragmentDirections.actionEvalDisciplineToRating(args.code, args.department)
            findNavController().navigate(directions)
        }
    }

    private fun handleData(resource: Resource<EvaluationDiscipline>) {
        val data = resource.data
        if (data != null) {
            Timber.d("The data is $data")
            if (data.participant == true) {
                binding.btnEvaluate.show()
                binding.btnEvaluate.extend()
            }
            val teachers = data.teachers
            val evaluation = if (teachers != null) {
                DisciplineEvaluation(
                    data.name,
                    data.departmentName ?: data.department,
                    data.qtdStudents,
                    teachers.groupBy { it.semesterSystemId }.entries.map { entry ->
                        val key = entry.key
                        val semester = entry.value
                        val mean = semester.sumByDouble { it.mean } / semester.size
                        val first = semester.first()
                        SemesterMean(key, first.semester, mean)
                    }.sortedBy { id * -1 },
                    teachers.groupBy { it.teacherId }.entries.map { entry ->
                        val appearances = entry.value
                        val appear = appearances.maxByOrNull { it.semesterSystemId }!!
                        val mean = appearances.sumByDouble { it.mean } / appearances.size
                        TeacherInt(appear.teacherId, appear.name, appear.semester, mean)
                    }.sortedBy { it.name }
                )
            } else {
                DisciplineEvaluation(data.name, data.departmentName ?: data.department, data.qtdStudents, listOf(), listOf())
            }
            elements.discipline = evaluation
        }
        when (resource.status) {
            Status.ERROR -> {
                binding.itemsRecycler.visibility = GONE
                binding.loadingGroup.visibility = GONE
                binding.failedGroup.visibility = VISIBLE
            }
            Status.LOADING -> {
                binding.itemsRecycler.visibility = GONE
                binding.loadingGroup.visibility = VISIBLE
                binding.failedGroup.visibility = GONE
            }
            Status.SUCCESS -> {
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
    val id: Long,
    val name: String,
    val lastSeen: String,
    val mean: Double
)
