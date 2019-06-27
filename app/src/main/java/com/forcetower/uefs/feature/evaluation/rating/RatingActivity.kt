package com.forcetower.uefs.feature.evaluation.rating

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.navArgs
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.ActivityEvaluationRatingBinding
import com.forcetower.uefs.feature.shared.FragmentAdapter
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

class RatingActivity : UActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    private lateinit var viewModel: EvaluationRatingViewModel
    private lateinit var binding: ActivityEvaluationRatingBinding
    private lateinit var adapter: FragmentAdapter
    private val args by navArgs<RatingActivityArgs>()
    private var currentData: List<Question>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_evaluation_rating)
        viewModel = provideViewModel(factory)
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

        viewModel.nextQuestion.observe(this, EventObserver {
            val position = binding.viewPager.currentItem
            val size = currentData?.size ?: 0
            if (position + 1 >= size) {
                finish()
            } else {
                binding.viewPager.setCurrentItem(position + 1, true)
            }
        })
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

    override fun supportFragmentInjector() = fragmentInjector
}