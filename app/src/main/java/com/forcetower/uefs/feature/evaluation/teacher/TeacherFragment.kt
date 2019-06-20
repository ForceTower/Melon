package com.forcetower.uefs.feature.evaluation.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.service.EvaluationTeacher
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentEvaluateTeacherBinding
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import javax.inject.Inject

class TeacherFragment : UFragment(), Injectable {
    private lateinit var binding: FragmentEvaluateTeacherBinding
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: EvaluationViewModel
    private lateinit var adapter: TeacherAdapter
    private val args: TeacherFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        adapter = TeacherAdapter(viewModel)
        return FragmentEvaluateTeacherBinding.inflate(inflater, container, false).apply {
            recyclerDisciplines.adapter = this@TeacherFragment.adapter
        }.also {
            binding = it
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.getTeacher(args.teacherId).observe(this, Observer { handleData(it) })
        viewModel.disciplineSelect.observe(this, Observer {
            val directions = TeacherFragmentDirections.actionEvalTeacherToRating()
            findNavController().navigate(directions)
        })
    }

    private fun handleData(resource: Resource<EvaluationTeacher>) {
        val data = resource.data
        when (resource.status) {
            Status.LOADING -> binding.loading = true
            Status.SUCCESS -> binding.loading = false
            Status.ERROR -> {
                binding.loading = true
                binding.failed = true
            }
        }
        if (data != null) {
            adapter.discipline = data
            binding.teacher = data
            binding.loading = false
            binding.failed = false
        }

        binding.executePendingBindings()
    }
}
