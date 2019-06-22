package com.forcetower.uefs.feature.evaluation.rating

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.DialogEvaluationCompletedQuestionBinding
import com.forcetower.uefs.databinding.DialogEvaluationInternalQuestionBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import javax.inject.Inject

class InternalQuestionFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: EvaluationRatingViewModel
    private lateinit var binding: DialogEvaluationInternalQuestionBinding
    private lateinit var question: Question

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        val args = requireNotNull(arguments)
        val last = args.getBoolean("last", false)
        question = Question(args.getLong("id"), args.getString("question")!!, args.getString("description"), teacher = false, discipline = false, last = last)
        return if (!last) {
            DialogEvaluationInternalQuestionBinding.inflate(inflater, container, false).also {
                binding = it
            }.apply {
                question = this@InternalQuestionFragment.question
            }.root
        } else {
            DialogEvaluationCompletedQuestionBinding.inflate(inflater, container, false).apply {
                btnComplete.setOnClickListener {
                    viewModel.onNextQuestion()
                }
            }.root
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.continueQuestion.setOnClickListener {
            val rating = binding.rating.rating
            viewModel.answer(question.id, rating)
            viewModel.onNextQuestion()
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
