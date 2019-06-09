package com.forcetower.uefs.feature.evaluation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.service.EvaluationHomeTopic
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentEvaluationHomeBinding
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import timber.log.Timber
import javax.inject.Inject

class HomeFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: EvaluationViewModel
    private lateinit var binding: FragmentEvaluationHomeBinding
    private lateinit var adapter: EvaluationTopicAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        adapter = EvaluationTopicAdapter()
        return FragmentEvaluationHomeBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@HomeFragment
            disciplinesRecycler.apply {
                adapter = this@HomeFragment.adapter
                itemAnimator?.run {
                    addDuration = 120L
                    moveDuration = 120L
                    changeDuration = 120L
                    removeDuration = 100L
                }
            }
        }.also {
            binding = it
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = provideActivityViewModel(factory)
        viewModel.getAccount().observe(this, Observer { handleAccount(it) })
        viewModel.getTrendingList().observe(this, Observer { handleTopics(it) })
    }

    private fun handleTopics(resource: Resource<List<EvaluationHomeTopic>>) {
        val data = resource.data
        when (resource.status) {
            Status.LOADING -> {
                if (data == null) {
                    binding.loadingGroup.visibility = VISIBLE
                    binding.disciplinesRecycler.visibility = GONE
                } else {
                    binding.loadingGroup.visibility = GONE
                    binding.disciplinesRecycler.visibility = VISIBLE
                }
            }
            Status.SUCCESS -> {
                if (data != null) {
                    binding.loadingGroup.visibility = GONE
                    binding.disciplinesRecycler.visibility = VISIBLE
                }
            }
            Status.ERROR -> {
                if (data != null) {
                    binding.loadingGroup.visibility = GONE
                    binding.disciplinesRecycler.visibility = VISIBLE
                } else {
                    showSnack(getString(R.string.evaluation_topics_load_failed))
                }
            }
        }
        if (data != null) {
            Timber.d("Data received $data")
            adapter.currentList = data
        }
    }

    private fun handleAccount(resource: Resource<Account>) {
        val account = resource.data ?: return
        binding.account = account
        binding.executePendingBindings()
    }
}