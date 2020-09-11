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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.databinding.DialogEvaluationCompletedQuestionBinding
import com.forcetower.uefs.databinding.DialogEvaluationInternalQuestionBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InternalQuestionFragment : UFragment() {
    private val viewModel: EvaluationRatingViewModel by activityViewModels()
    private var question: Question? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val args = requireNotNull(arguments)
        val last = args.getBoolean("last", false)
        question = Question(args.getLong("id"), args.getString("question")!!, args.getString("description"), teacher = false, discipline = false, last = last)
        return if (!last) {
            DialogEvaluationInternalQuestionBinding.inflate(inflater, container, false).apply {
                question = this@InternalQuestionFragment.question
                continueQuestion.setOnClickListener {
                    val rating = rating.rating
                    viewModel.answer(question!!.id, rating)
                    viewModel.onNextQuestion()
                }
            }.root
        } else {
            DialogEvaluationCompletedQuestionBinding.inflate(inflater, container, false).apply {
                btnComplete.setOnClickListener {
                    viewModel.onNextQuestion()
                }
            }.root
        }
    }

    companion object {
        fun newInstance(question: Question): InternalQuestionFragment {
            return InternalQuestionFragment().apply {
                arguments = bundleOf(
                    "id" to question.id,
                    "question" to question.question,
                    "description" to question.description,
                    "last" to question.last
                )
            }
        }
    }
}
