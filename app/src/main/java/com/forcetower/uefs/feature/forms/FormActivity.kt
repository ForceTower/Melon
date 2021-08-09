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

package com.forcetower.uefs.feature.forms

import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.databinding.ActivityFormsBinding
import com.forcetower.uefs.feature.shared.FragmentAdapter
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.themeswitcher.ThemeOverlayUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class FormActivity : UActivity() {
    private val viewModel: FormsViewModel by viewModels()
    private lateinit var binding: ActivityFormsBinding
    private lateinit var adapter: FragmentAdapter
    private lateinit var currentData: List<Question>

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeOverlayUtils.applyThemeOverlays(this, intArrayOf(R.id.theme_feature_background_color))
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_forms)
        adapter = FragmentAdapter(supportFragmentManager)
        binding.viewPager.adapter = adapter

        viewModel.account.observe(
            this,
            Observer {
                try {
                    viewModel.answer("entry.182173763", it?.grouping?.toString() ?: "0")
                } catch (error: Throwable) {
                    Timber.e(error)
                }
            }
        )

        val questions = createQuestions()
        currentData = questions
        createFragmentsList(questions)

        viewModel.nextQuestion.observe(
            this,
            EventObserver {
                val position = binding.viewPager.currentItem
                val size = currentData.size
                val nextPos = position + 1
                if (nextPos >= size) {
                    viewModel.submitAnswers()
                    finish()
                } else {
                    binding.viewPager.setCurrentItem(nextPos, true)
                }
            }
        )
    }

    private fun createQuestions(): List<Question> {
        return listOf(
            Question(
                0,
                "O quão satisfeito você está com o Aplicativo UNES?",
                "Onde 1 significa pouco satisfeito e 5 significa muito satisfeito",
                teacher = false,
                discipline = false,
                formId = "entry.745006824"
            ),
            Question(
                1,
                "O quanto você está incomodado com a forma como o Aplicativo UNES exibe os anúncios atualmente?",
                "Onde 1 significa não incomoda e 5 significa incomoda muito",
                teacher = false,
                discipline = false,
                formId = "entry.917493162"
            ),
            Question(
                2,
                "O quanto você recomendaria o Aplicativo UNES para um colega?",
                "Onde 1 significa de jeito nenhum e 5 com certeza recomendo",
                teacher = false,
                discipline = false,
                formId = "entry.1382232665"
            )
        )
    }

    override fun shouldApplyThemeOverlay() = false

    private fun createFragmentsList(data: List<Question>) {
        val fragments = data.map { InternalFormFragment.newInstance(it) }
        adapter.setItems(fragments)
    }
}
