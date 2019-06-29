package com.forcetower.uefs.feature.flowchart.discipline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
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
        binding.discipline = disciplineMocked
        binding.semesterValue = semesterMocked
        adapter.currentList = requirementsMocked
        viewModel.onDisciplineSelect.observe(this, EventObserver { onDisciplineSelected(it) })
    }

    private fun onDisciplineSelected(discipline: FlowchartDisciplineUI) {
        val direction = DisciplineFragmentDirections.actionDisciplineSelf()
        findNavController().navigate(direction)
    }

    private fun onUpPressed() {
        findNavController().navigateUp()
    }

    companion object {
        val disciplineMocked = FlowchartDisciplineUI(1, "Obrigatória", true, "Sinais e Sistemas", "EXA856", 30, "Departamento de Ciencias Exatas", "Apresenta o curso de engenharia de computação e suas áreas.\n\nAlém disso varias coisas aleatorias são mostradas nesta aula.")
        const val semesterMocked = "Primeiro Semestre"
        val requirementsMocked = listOf(
            FlowchartDisciplineUI(4, "Pré Requisitos", true, "Física III", "EXA856", 30, "Departamento de Ciencias Exatas", "Apresenta o curso de engenharia de computação e suas áreas.\n\nAlém disso varias coisas aleatorias são mostradas nesta aula."),
            FlowchartDisciplineUI(8, "Pré Requisitos", true, "Métodos Numéricos", "EXA856", 30, "Departamento de Ciencias Exatas", "Apresenta o curso de engenharia de computação e suas áreas.\n\nAlém disso varias coisas aleatorias são mostradas nesta aula."),
            FlowchartDisciplineUI(9, "Có Requisitos", true, "Circuitos Elétricos", "EXA856", 30, "Departamento de Ciencias Exatas", "Apresenta o curso de engenharia de computação e suas áreas.\n\nAlém disso varias coisas aleatorias são mostradas nesta aula."),
            FlowchartDisciplineUI(10, "Có Requisitos", true, "MI - Circuitos Elétricos", "EXA856", 60, "Departamento de Ciencias Exatas", "Apresenta o curso de engenharia de computação e suas áreas.\n\nAlém disso varias coisas aleatorias são mostradas nesta aula."),
            FlowchartDisciplineUI(11, "Desbloqueia", true, "MI - Processamento Digital de Sinais", "EXA856", 60, "Departamento de Ciencias Exatas", "Apresenta o curso de engenharia de computação e suas áreas.\n\nAlém disso varias coisas aleatorias são mostradas nesta aula."),
            FlowchartDisciplineUI(12, "Desbloqueia", true, "Processamento Digital de Sinais", "EXA856", 60, "Departamento de Ciencias Exatas", "Apresenta o curso de engenharia de computação e suas áreas.\n\nAlém disso varias coisas aleatorias são mostradas nesta aula."),
            FlowchartDisciplineUI(13, "Desbloqueia", true, "Eletrônica para Processamento Digital de Sinais", "EXA856", 60, "Departamento de Ciencias Exatas", "Apresenta o curso de engenharia de computação e suas áreas.\n\nAlém disso varias coisas aleatorias são mostradas nesta aula.")
        )
    }
}