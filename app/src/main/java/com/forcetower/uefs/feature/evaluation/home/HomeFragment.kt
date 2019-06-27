package com.forcetower.uefs.feature.evaluation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.service.EvaluationDiscipline
import com.forcetower.uefs.core.model.service.EvaluationHomeTopic
import com.forcetower.uefs.core.model.service.EvaluationTeacher
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.EventObserver
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
        viewModel = provideActivityViewModel(factory)
        adapter = EvaluationTopicAdapter(viewModel)
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
            btnSearchEverything.setOnClickListener { navigateToSearch() }
        }.also {
            binding = it
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.getAccount().observe(this, Observer { handleAccount(it) })
        viewModel.getTrendingList().observe(this, Observer { handleTopics(it) })
        viewModel.disciplineSelect.observe(this, EventObserver { onDisciplineSelected(it) })
        viewModel.teacherSelect.observe(this, EventObserver { onTeacherSelected(it) })
    }

    private fun onTeacherSelected(teacher: EvaluationTeacher) {
        val id = teacher.teacherId
        val directions = HomeFragmentDirections.actionHomeToEvalTeacher(id)
        findNavController().navigate(directions)
    }

    private fun onDisciplineSelected(discipline: EvaluationDiscipline) {
        val code = discipline.code
        val department = discipline.department
        val directions = HomeFragmentDirections.actionHomeToEvalDiscipline(code, department)
        findNavController().navigate(directions)
    }

    private fun navigateToSearch() {
        val directions = HomeFragmentDirections.actionHomeToSearch()
        findNavController().navigate(directions)
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