package com.forcetower.uefs.feature.evaluation.rating

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.DialogEvaluationRatingBinding
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.shared.FragmentAdapter
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import javax.inject.Inject

class RatingDialog : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: EvaluationViewModel
    private lateinit var binding: DialogEvaluationRatingBinding
    private lateinit var adapter: FragmentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        adapter = FragmentAdapter(childFragmentManager)
        return DialogEvaluationRatingBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewPager.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.getQuestionsForTeacher().observe(this, Observer { useResponse(it) })
    }

    private fun useResponse(resource: Resource<List<Question>>) {
        val data = resource.data
        if (data != null) {
            binding.groupLoading.visibility = GONE
            binding.viewPager.visibility = VISIBLE

            createFragmentsList(data)
        } else {
            // todo error handle
            binding.groupLoading.visibility = VISIBLE
            binding.viewPager.visibility = GONE
        }
    }

    private fun createFragmentsList(data: List<Question>) {
        val fragments = data.map { InternalQuestionFragment.newInstance(it) }
        adapter.setItems(fragments)
    }
}