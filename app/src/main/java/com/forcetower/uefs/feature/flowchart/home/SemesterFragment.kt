package com.forcetower.uefs.feature.flowchart.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentFlowchartSemestersBinding
import com.forcetower.uefs.feature.flowchart.FlowchartViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import javax.inject.Inject

class SemesterFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var binding: FragmentFlowchartSemestersBinding
    private lateinit var viewModel: FlowchartViewModel
    private lateinit var adapter: SemesterAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        displayName = getString(R.string.flowchart_semester)
        viewModel = provideViewModel(factory)
        adapter = SemesterAdapter()
        return FragmentFlowchartSemestersBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            flowSemesterRecycler.adapter = adapter
        }.root
    }

    companion object {
        fun newInstance(): SemesterFragment = SemesterFragment()
    }
}