package com.forcetower.uefs.feature.flowchart.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI
import com.forcetower.uefs.core.util.toJson
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentFlowchartSemestersBinding
import com.forcetower.uefs.feature.flowchart.FlowchartViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import timber.log.Timber
import javax.inject.Inject

class SemestersFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var binding: FragmentFlowchartSemestersBinding
    private lateinit var viewModel: FlowchartViewModel
    private lateinit var adapter: SemesterAdapter

    init {
        displayName = "Semestres"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        adapter = SemesterAdapter(viewModel)
        viewModel.setCourse(1)
        return FragmentFlowchartSemestersBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            flowSemesterRecycler.adapter = adapter
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.flowchart.observe(this, Observer { onSemestersReceived(it) })
        viewModel.onSemesterSelect.observe(this, EventObserver { onSemesterSelected(it) })
    }

    private fun onSemestersReceived(values: List<FlowchartSemesterUI>) {
        adapter.submitList(values)
    }

    private fun onSemesterSelected(semester: FlowchartSemesterUI) {
        Timber.d("Semester selected ${semester.toJson()}")
        val direction = FlowchartFragmentDirections.actionHomeToSemester(semester.id)
        findNavController().navigate(direction)
    }

    companion object {
        fun newInstance(): SemestersFragment = SemestersFragment()
    }
}