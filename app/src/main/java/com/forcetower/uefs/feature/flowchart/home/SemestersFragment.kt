package com.forcetower.uefs.feature.flowchart.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        return FragmentFlowchartSemestersBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            flowSemesterRecycler.adapter = adapter
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter.submitList(mockedList)
        viewModel.onSemesterSelect.observe(this, EventObserver { onSemesterSelected(it) })
    }

    private fun onSemesterSelected(semester: FlowchartSemesterUI) {
        Timber.d("Semester selected ${semester.toJson()}")
        val direction = FlowchartFragmentDirections.actionHomeToSemester()
        findNavController().navigate(direction)
    }

    companion object {
        fun newInstance(): SemestersFragment = SemestersFragment()

        private val mockedList = listOf(
            FlowchartSemesterUI(1, 1, 1, "Primeiro Semestre", 480, 8),
            FlowchartSemesterUI(2, 1, 2, "Segundo Semestre", 420, 7),
            FlowchartSemesterUI(3, 1, 3, "Terceiro Semestre", 360, 6)
        )
    }
}