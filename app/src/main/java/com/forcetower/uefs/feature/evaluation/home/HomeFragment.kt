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
import com.forcetower.uefs.core.model.edge.paradox.EvaluationHotTopic
import com.forcetower.uefs.core.model.edge.paradox.PublicHotEvaluationDiscipline
import com.forcetower.uefs.core.model.edge.paradox.PublicHotEvaluationTeacher
import com.forcetower.uefs.core.model.unes.EdgeServiceAccount
import com.forcetower.uefs.databinding.FragmentEvaluationHomeBinding
import com.forcetower.uefs.feature.evaluation.EvaluationState
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

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
        viewModel.getAccount().observe(viewLifecycleOwner) { handleAccount(it) }
        viewModel.trending.observe(viewLifecycleOwner) { handleTopics(it) }
        viewModel.disciplineSelect.observe(viewLifecycleOwner, EventObserver { onDisciplineSelected(it) })
        viewModel.teacherSelect.observe(viewLifecycleOwner, EventObserver { onTeacherSelected(it) })
        viewModel.state.observe(viewLifecycleOwner, ::handleState)

        if (savedInstanceState == null) {
            viewModel.fetchTrending()
        }
    }

    private fun onTeacherSelected(teacher: PublicHotEvaluationTeacher) {
        val directions = HomeFragmentDirections.actionHomeToEvalTeacher(teacher.id)
        findNavController().navigate(directions)
    }

    private fun onDisciplineSelected(discipline: PublicHotEvaluationDiscipline) {
        val directions = HomeFragmentDirections.actionHomeToEvalDiscipline(discipline.id)
        findNavController().navigate(directions)
    }

    private fun navigateToSearch() {
        val directions = HomeFragmentDirections.actionHomeToSearch()
        findNavController().navigate(directions)
    }

    private fun handleTopics(data: List<EvaluationHotTopic>) {
        binding.loadingGroup.visibility = GONE
        binding.disciplinesRecycler.visibility = VISIBLE
        adapter.currentList = data
    }

    private fun handleState(state: EvaluationState) {
        when {
            state.loading -> {
                binding.loadingGroup.visibility = VISIBLE
                binding.disciplinesRecycler.visibility = GONE
            }
            state.failed -> {
                showSnack(getString(R.string.evaluation_topics_load_failed))
            }
        }
    }

    private fun handleAccount(account: EdgeServiceAccount?) {
        account ?: return
        binding.account = account
    }
}
