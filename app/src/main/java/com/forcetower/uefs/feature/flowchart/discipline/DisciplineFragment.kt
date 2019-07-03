package com.forcetower.uefs.feature.flowchart.discipline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.core.model.unes.FlowchartRequirementUI
import com.forcetower.uefs.core.model.unes.FlowchartSemester
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentFlowchartDisciplineDetailsBinding
import com.forcetower.uefs.feature.flowchart.FlowchartViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import javax.inject.Inject

class DisciplineFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var binding: FragmentFlowchartDisciplineDetailsBinding
    private lateinit var viewModel: FlowchartViewModel
    private lateinit var adapter: DisciplineDetailsAdapter
    private val args: DisciplineFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        adapter = DisciplineDetailsAdapter(viewModel)
        return FragmentFlowchartDisciplineDetailsBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            up.setOnClickListener { onUpPressed() }
            disciplineDetailsRecycler.adapter = adapter
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.getDisciplineUi(args.disciplineId).observe(this, Observer { onReceiveDiscipline(it) })
        viewModel.getSemesterName(args.disciplineId).observe(this, Observer { onReceiveSemesterName(it) })
        viewModel.getRequirementsUI(args.disciplineId).observe(this, Observer { onReceiveRequirements(it) })
//        viewModel.getRecursiveRequirementsUI(args.disciplineId).observe(this, Observer { onReceiveRecursive(it) })
//        viewModel.getRecursiveUnlockRequirementUI(args.disciplineId).observe(this, Observer { onReceiveUnlockRecursive(it) })
        viewModel.onRequirementSelect.observe(this, EventObserver { onRequirementSelected(it) })
    }

    private fun onReceiveUnlockRecursive(unlock: List<FlowchartRequirementUI>) {
        adapter.unlock = unlock
    }

    private fun onReceiveRecursive(requirements: List<FlowchartRequirementUI>) {
        adapter.block = requirements
    }

    private fun onReceiveRequirements(requirements: List<FlowchartRequirementUI>) {
        adapter.currentList = requirements
    }

    private fun onReceiveSemesterName(semester: FlowchartSemester?) {
        binding.semesterValue = semester?.name
    }

    private fun onReceiveDiscipline(discipline: FlowchartDisciplineUI?) {
        binding.discipline = discipline
    }

    private fun onRequirementSelected(requirement: FlowchartRequirementUI) {
        if (requirement.requiredDisciplineId != null) {
            val id = if (requirement.type == getString(R.string.flowchart_recursive_unlock))
                requirement.disciplineId
            else
                requirement.requiredDisciplineId
            val direction = DisciplineFragmentDirections.actionDisciplineSelf(id)
            findNavController().navigate(direction)
        }
    }

    private fun onUpPressed() {
        findNavController().navigateUp()
    }
}