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

package com.forcetower.uefs.feature.evaluation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.EvaluationDiscipline
import com.forcetower.uefs.core.model.service.EvaluationHomeTopic
import com.forcetower.uefs.core.model.service.EvaluationTeacher
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.databinding.FragmentEvaluationHomeBinding
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : UFragment() {
    private val viewModel: EvaluationViewModel by activityViewModels()
    private lateinit var binding: FragmentEvaluationHomeBinding
    private lateinit var adapter: EvaluationTopicAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        adapter = EvaluationTopicAdapter(viewModel)
        return FragmentEvaluationHomeBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@HomeFragment
            disciplinesRecycler.apply {
                adapter = this@HomeFragment.adapter
                itemAnimator?.run {
                    addDuration = 120L
                    moveDuration = 120L
                    changeDuration = 120L
                    removeDuration = 100L
                }
            }
            btnSearchEverything.setOnClickListener { navigateToSearch() }
        }.also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getAccount().observe(viewLifecycleOwner, { handleAccount(it) })
        viewModel.getTrendingList().observe(viewLifecycleOwner, { handleTopics(it) })
        viewModel.disciplineSelect.observe(viewLifecycleOwner, EventObserver { onDisciplineSelected(it) })
        viewModel.teacherSelect.observe(viewLifecycleOwner, EventObserver { onTeacherSelected(it) })
    }

    private fun onTeacherSelected(teacher: EvaluationTeacher) {
        val id = teacher.teacherId
        val directions = HomeFragmentDirections.actionHomeToEvalTeacher(id, null)
        findNavController().navigate(directions)
    }

    private fun onDisciplineSelected(discipline: EvaluationDiscipline) {
        val code = discipline.code
        val department = discipline.department
        val directions = HomeFragmentDirections.actionHomeToEvalDiscipline(code, department)
        findNavController().navigate(directions)
    }

    private fun navigateToSearch() {
        val directions = HomeFragmentDirections.actionHomeToSearch()
        findNavController().navigate(directions)
    }

    private fun handleTopics(resource: Resource<List<EvaluationHomeTopic>>) {
        val data = resource.data
        when (resource.status) {
            Status.LOADING -> {
                if (data == null) {
                    binding.loadingGroup.visibility = VISIBLE
                    binding.disciplinesRecycler.visibility = GONE
                } else {
                    binding.loadingGroup.visibility = GONE
                    binding.disciplinesRecycler.visibility = VISIBLE
                }
            }
            Status.SUCCESS -> {
                if (data != null) {
                    binding.loadingGroup.visibility = GONE
                    binding.disciplinesRecycler.visibility = VISIBLE
                }
            }
            Status.ERROR -> {
                if (data != null) {
                    binding.loadingGroup.visibility = GONE
                    binding.disciplinesRecycler.visibility = VISIBLE
                } else {
                    showSnack(getString(R.string.evaluation_topics_load_failed))
                }
            }
        }
        if (data != null) {
            Timber.d("Data received $data")
            adapter.currentList = data
        }
    }

    private fun handleAccount(resource: Resource<Account>) {
        val account = resource.data ?: return
        binding.account = account
        binding.executePendingBindings()
    }
}
