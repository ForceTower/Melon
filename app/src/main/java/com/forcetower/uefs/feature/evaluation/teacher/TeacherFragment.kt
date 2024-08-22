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

package com.forcetower.uefs.feature.evaluation.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.core.model.edge.paradox.PublicTeacherEvaluationCombinedData
import com.forcetower.uefs.databinding.FragmentEvaluateTeacherBinding
import com.forcetower.uefs.feature.evaluation.EvaluationState
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeacherFragment : UFragment() {
    private lateinit var binding: FragmentEvaluateTeacherBinding
    private lateinit var adapter: TeacherAdapter
    private val viewModel: EvaluationViewModel by viewModels()
    private val args: TeacherFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        adapter = TeacherAdapter(viewModel)
        return FragmentEvaluateTeacherBinding.inflate(inflater, container, false).apply {
            btnEvaluate.hide()
            recyclerDisciplines.adapter = this@TeacherFragment.adapter
        }.also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.fetchTeacher(args.id)
        viewModel.teacher.observe(viewLifecycleOwner) { handleData(it) }
        viewModel.state.observe(viewLifecycleOwner) { handleState(it) }
        viewModel.disciplineSelect.observe(
            viewLifecycleOwner,
            EventObserver {
                val directions = TeacherFragmentDirections.actionTeacherToDiscipline(it.id)
                findNavController().navigate(directions)
            }
        )
        viewModel.disciplineSelectTeacher.observe(
            viewLifecycleOwner,
            EventObserver {
                val directions = TeacherFragmentDirections.actionTeacherToDiscipline(it.disciplineId)
                findNavController().navigate(directions)
            }
        )
//        binding.btnEvaluate.setOnClickListener {
//            val directions = TeacherFragmentDirections.actionEvalTeacherToRating(args.teacherId)
//            findNavController().navigate(directions)
//        }
    }

    private fun handleData(data: PublicTeacherEvaluationCombinedData) {
        adapter.discipline = data
        binding.run {
            teacher = data
            loading = false
            failed = false
        }
    }

    private fun handleState(state: EvaluationState) {
        when {
            state.loading -> binding.loading = true
            state.failed -> {
                binding.loading = true
                binding.failed = true
            }
            else -> binding.loading = false
        }
    }
}
