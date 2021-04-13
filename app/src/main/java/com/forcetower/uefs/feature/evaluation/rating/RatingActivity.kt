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

package com.forcetower.uefs.feature.evaluation.rating

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.navArgs
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.databinding.ActivityEvaluationRatingBinding
import com.forcetower.uefs.feature.shared.FragmentAdapter
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.themeswitcher.ThemeOverlayUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RatingActivity : UActivity() {
    private val viewModel: EvaluationRatingViewModel by viewModels()
    private lateinit var binding: ActivityEvaluationRatingBinding
    private lateinit var adapter: FragmentAdapter
    private val args by navArgs<RatingActivityArgs>()
    private var currentData: List<Question>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeOverlayUtils.applyThemeOverlays(this, intArrayOf(R.id.theme_feature_background_color))
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_evaluation_rating)
        adapter = FragmentAdapter(supportFragmentManager)
        binding.viewPager.adapter = adapter

        if (args.isTeacher) {
            viewModel.initForTeacher(args.teacherId)
            viewModel.getQuestionsForTeacher(args.teacherId).observe(this, Observer { useResponse(it) })
        } else {
            val code = args.code ?: "0"
            val department = args.department ?: "0"
            viewModel.initForDiscipline(code, department)
            viewModel.getQuestionsForDiscipline(code, department).observe(this, Observer { useResponse(it) })
        }

        viewModel.nextQuestion.observe(
            this,
            EventObserver {
                val position = binding.viewPager.currentItem
                val size = currentData?.size ?: 0
                if (position + 1 >= size) {
                    finish()
                } else {
                    binding.viewPager.setCurrentItem(position + 1, true)
                }
            }
        )
    }

    private fun useResponse(resource: Resource<List<Question>>) {
        val data = resource.data
        if (data != null) {
            val additional = data.toMutableList().apply { add(Question(-2, "", "", last = true, teacher = false, discipline = false)) }
            currentData = additional
            binding.groupLoading.visibility = GONE
            binding.viewPager.visibility = VISIBLE
            createFragmentsList(additional)
        } else {
            binding.groupLoading.visibility = VISIBLE
            binding.viewPager.visibility = GONE
        }
    }

    private fun createFragmentsList(data: List<Question>) {
        val fragments = data.map { InternalQuestionFragment.newInstance(it) }
        adapter.setItems(fragments)
    }

    override fun shouldApplyThemeOverlay() = false
}
