package com.forcetower.uefs.feature.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.databinding.DialogEvaluationInternalQuestionBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InternalFormFragment : UFragment() {
    private val viewModel: FormsViewModel by activityViewModels()
    private lateinit var question: Question

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val args = requireNotNull(arguments)
        val last = args.getBoolean("last", false)
        question = Question(args.getLong("id"), args.getString("question")!!, args.getString("description"), teacher = false, discipline = false, last = last, formId = args.getString("formId"))
        return DialogEvaluationInternalQuestionBinding.inflate(inflater, container, false).apply {
            continueQuestion.isEnabled = false
            question = this@InternalFormFragment.question
            continueQuestion.setOnClickListener {
                val immutable = question!!
                val rating = rating.rating
                val formId = immutable.formId
                val questionId = immutable.id.toString()
                val id = formId ?: questionId
                if (rating > 0) {
                    viewModel.answer(id, rating)
                    viewModel.onNextQuestion()
                }
            }
            rating.setOnRatingBarChangeListener { _, rating, _ ->
                if (rating > 0) {
                    continueQuestion.isEnabled = true
                }
            }
        }.root
    }
    companion object {
        fun newInstance(question: Question): InternalFormFragment {
            return InternalFormFragment().apply {
                arguments = bundleOf(
                    "id" to question.id,
                    "question" to question.question,
                    "description" to question.description,
                    "last" to question.last,
                    "formId" to question.formId
                )
            }
        }
    }
}