package com.forcetower.uefs.feature.flowchart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentFlowchartSelectCourseBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import timber.log.Timber
import javax.inject.Inject

class SelectCourseFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: FlowchartViewModel
    private lateinit var binding: FragmentFlowchartSelectCourseBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        return FragmentFlowchartSelectCourseBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val coursesAdapter = CourseAdapter(viewModel)
        binding.recyclerCourses.apply {
            adapter = coursesAdapter
        }

        viewModel.getFlowcharts().observe(this, Observer {
            Timber.d("Resource data ${it.data}")
            if (it.data != null) {
                coursesAdapter.submitList(it.data)
            }
        })

        viewModel.onFlowchartSelect.observe(this, EventObserver {
            val direction = SelectCourseFragmentDirections.actionSelectToStart(it.courseId)
            findNavController().navigate(direction)
        })
    }
}