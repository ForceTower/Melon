package com.forcetower.uefs.feature.flowchart.semester

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentFlowchartSemesterBinding
import com.forcetower.uefs.feature.flowchart.FlowchartViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import javax.inject.Inject

class SemesterFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var binding: FragmentFlowchartSemesterBinding
    private lateinit var viewModel: FlowchartViewModel
    private lateinit var adapter: DisciplinesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        adapter = DisciplinesAdapter(viewModel)
        return FragmentFlowchartSemesterBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            recyclerDisciplines.adapter = adapter
            up.setOnClickListener { backToRoot() }
        }.root
    }

    private fun backToRoot() {
        findNavController().navigateUp()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.semester = semesterMock
        adapter.submitList(disciplinesMock)

        viewModel.onDisciplineSelect.observe(this, EventObserver { onDisciplineSelected(it) })
    }

    private fun onDisciplineSelected(discipline: FlowchartDisciplineUI) {
        val direction = SemesterFragmentDirections.actionSemesterToDiscipline()
        findNavController().navigate(direction)
    }

    companion object {
        private val semesterMock = FlowchartSemesterUI(1, 1, 1, "Primeiro Semestre", 480, 8)
        private val disciplinesMock = listOf(
            FlowchartDisciplineUI(1, "Obrigatória", true, "Introdução à Engenharia de Computação", "EXA856", 30, "Departamento de Ciencias Exatas", "Apresenta o curso de engenharia de computação e suas áreas"),
            FlowchartDisciplineUI(2, "Obrigatória", true, "Algoritmos e Programação I", "EXA856", 60, "Departamento de Ciencias Exatas", "Conceitos de Algoritmos e tal"),
            FlowchartDisciplineUI(3, "Obrigatória", true, "Produção de Textos Tecnicos e Acadêmicos", "EXA856", 30, "Departamento de Tecnologia", "Rosária ensinando como escrever um bom texto e como formatar o texto")
        )
    }
}