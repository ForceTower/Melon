package com.forcetower.uefs.feature.evaluation.rating

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.databinding.DialogEvaluationInternalQuestionBinding
import com.forcetower.uefs.feature.shared.UFragment

class InternalQuestionFragment : UFragment(), Injectable {
    private lateinit var binding: DialogEvaluationInternalQuestionBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val args = requireNotNull(arguments)
        val quest = Question(args.getLong("id"), args.getString("question")!!, args.getString("description"), teacher = false, discipline = false)
        return DialogEvaluationInternalQuestionBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            question = quest
        }.root
    }

    companion object {
        fun newInstance(question: Question): InternalQuestionFragment {
            return InternalQuestionFragment().apply {
                arguments = bundleOf(
                    "id" to question.id,
                    "question" to question.question,
                    "description" to question.description
                )
            }
        }
    }
}
