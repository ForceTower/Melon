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

package com.forcetower.uefs.feature.evaluation.search

import android.app.ActivityOptions
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.EvaluationEntity
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.databinding.FragmentEvaluationSearchBinding
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.profile.ProfileActivity
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SearchFragment : UFragment() {
    private val viewModel: EvaluationViewModel by activityViewModels()
    private lateinit var binding: FragmentEvaluationSearchBinding
    private lateinit var adapter: EvaluationEntityAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        adapter = EvaluationEntityAdapter(viewModel)
        return FragmentEvaluationSearchBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            omniText.addTextChangedListener(
                object : TextWatcher {
                    override fun afterTextChanged(edit: Editable?) {
                        val text = edit?.toString() ?: ""
                        if (text.length >= 3 || text.isEmpty()) {
                            Timber.d("Querying $text")
                            viewModel.query(text)
                        }
                    }
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
                }
            )
            wildcardRecycler.adapter = adapter
            wildcardRecycler.itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.downloadDatabase().observe(viewLifecycleOwner, Observer { loadKnowledge(it) })
        viewModel.query.observe(
            viewLifecycleOwner,
            Observer {
                adapter.submitList(it)
            }
        )
        viewModel.entitySelect.observe(
            viewLifecycleOwner,
            EventObserver {
                onEvalEntitySelected(it)
            }
        )
    }

    private fun onEvalEntitySelected(entity: EvaluationEntity) {
        when (entity.type) {
            0 -> {
                val directions = SearchFragmentDirections.actionSearchToTeacher(entity.referencedId, null)
                findNavController().navigate(directions)
            }
            1 -> {
                val comp1 = entity.comp1 ?: return
                val comp2 = entity.comp2 ?: return
                val directions = SearchFragmentDirections.actionSearchToDiscipline(comp2, comp1)
                findNavController().navigate(directions)
            }
            2 -> {
                onStudentSelected(entity)
            }
        }
    }

    private fun onStudentSelected(entity: EvaluationEntity) {
        val shared = findStudentHeadshot(binding.wildcardRecycler, entity.referencedId)
        val option = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), shared, getString(R.string.student_headshot_transition))
        val intent = ProfileActivity.startIntent(requireContext(), entity.referencedId, entity.referenceLong1 ?: 0)
        startActivity(intent, option.toBundle())
    }

    private fun findStudentHeadshot(entities: ViewGroup, studentId: Long): View {
        entities.forEach {
            if (it.getTag(R.id.tag_student_id) == studentId) {
                return it.findViewById(R.id.entity_image)
            }
        }
        Timber.e("Could not find view for speaker id $studentId")
        return entities
    }

    private fun loadKnowledge(resource: Resource<Boolean>) {
        if (resource.status == Status.LOADING && resource.data == true) {
            binding.loadingGroup.visibility = VISIBLE
            binding.wildcardRecycler.visibility = GONE
        } else {
            binding.loadingGroup.visibility = GONE
            binding.wildcardRecycler.visibility = VISIBLE
        }
    }
}
