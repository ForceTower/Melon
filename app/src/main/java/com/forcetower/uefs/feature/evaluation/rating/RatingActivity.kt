package com.forcetower.uefs.feature.evaluation.rating

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.ActivityEvaluationRatingBinding
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.shared.FragmentAdapter
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import timber.log.Timber
import javax.inject.Inject

class RatingActivity : UActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    private lateinit var viewModel: EvaluationViewModel
    private lateinit var binding: ActivityEvaluationRatingBinding
    private lateinit var adapter: FragmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_evaluation_rating)
        viewModel = provideViewModel(factory)
        adapter = FragmentAdapter(supportFragmentManager)
        binding.viewPager.adapter = adapter

        viewModel.getQuestionsForTeacher().observe(this, Observer { useResponse(it) })
    }

    private fun useResponse(resource: Resource<List<Question>>) {
        val data = resource.data
        Timber.d("the resource ${resource.status}")
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
        Timber.d("The items: $data")
    }

    override fun supportFragmentInjector() = fragmentInjector
}