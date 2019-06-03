package com.forcetower.uefs.feature.evaluation.discipline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.databinding.FragmentEvaluationDisciplineBinding
import com.forcetower.uefs.feature.shared.UFragment

class DisciplineEvaluationFragment : UFragment() {
    private lateinit var binding: FragmentEvaluationDisciplineBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentEvaluationDisciplineBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val elements = EvaluationElementsAdapter()

        val evaluation = DisciplineEvaluation(
                "Metodologia da Pesquisa e Desenvolvimento em Engenharia de Computação",
                "Departamento de Tecnologia",
                32,
                listOf(
                        SemesterMean(0, "2013.1", 9.8),
                        SemesterMean(1, "2013.2", 6.1),
                        SemesterMean(2, "2014.1", 4.3),
                        SemesterMean(3, "2014.2", 5.6),
                        SemesterMean(4, "2015.1", 7.8),
                        SemesterMean(5, "2015.2", 3.2),
                        SemesterMean(6, "2016.1", 7.1)
                ),
                listOf(
                        TeacherInt(0, "Rosária da Paixão Trindade", "2016.1"),
                        TeacherInt(1, "João Batista Rocha Júnior", "2013.1"),
                        TeacherInt(2, "Daniel da Costa Silva", "2015.2")
                )
        )
        elements.discipline = evaluation

        binding.itemsRecycler.apply {
            adapter = elements
        }
    }
}

data class DisciplineEvaluation(
    val name: String,
    val department: String,
    val amount: Int,
    val grades: List<SemesterMean>,
    val teachers: List<TeacherInt> = emptyList()
)

data class SemesterMean(
    val id: Int,
    val name: String,
    val mean: Double
)

data class TeacherInt(
    val id: Int,
    val name: String,
    val lastSeen: String
)