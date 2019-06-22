package com.forcetower.uefs.feature.evaluation.discipline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.service.EvaluationDiscipline
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentEvaluationDisciplineBinding
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import timber.log.Timber
import javax.inject.Inject

class DisciplineEvaluationFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: EvaluationViewModel
    private lateinit var binding: FragmentEvaluationDisciplineBinding
    private lateinit var elements: EvaluationElementsAdapter
    private val args: DisciplineEvaluationFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        elements = EvaluationElementsAdapter(viewModel)
        return FragmentEvaluationDisciplineBinding.inflate(inflater, container, false).also {
            binding = it
            binding.btnEvaluate.hide(false)
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.getDiscipline(args.department, args.code).observe(this, Observer { handleData(it) })
        binding.itemsRecycler.apply {
            adapter = elements
            itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
        }
        viewModel.teacherIntSelect.observe(this, EventObserver {
            val directions = DisciplineEvaluationFragmentDirections.actionDisciplineToTeacher(it.id)
            findNavController().navigate(directions)
        })
        binding.btnEvaluate.setOnClickListener {
            val directions = DisciplineEvaluationFragmentDirections.actionEvalDisciplineToRating(args.code, args.department)
            findNavController().navigate(directions)
        }
    }

    private fun handleData(resource: Resource<EvaluationDiscipline>) {
        val data = resource.data
        if (data != null) {
            Timber.d("The data is $data")
            val teachers = data.teachers
            val evaluation = if (teachers != null) {
                DisciplineEvaluation(
                        data.name,
                        data.departmentName ?: data.department,
                        data.qtdStudents,
                        teachers.groupBy { it.semesterSystemId }.entries.map { entry ->
                            val key = entry.key
                            val semester = entry.value
                            val mean = semester.sumByDouble { it.mean } / semester.size
                            val first = semester.first()
                            SemesterMean(key, first.semester, mean)
                        }.sortedBy { id * -1 },
                        teachers.groupBy { it.teacherId }.entries.map { entry ->
                            val appearances = entry.value
                            val appear = appearances.maxBy { it.semesterSystemId }!!
                            val mean = appearances.sumByDouble { it.mean } / appearances.size
                            TeacherInt(appear.teacherId, appear.name, appear.semester, mean)
                        }.sortedBy { it.name }
                )
            } else {
                DisciplineEvaluation(data.name, data.departmentName ?: data.department, data.qtdStudents, listOf(), listOf())
            }
            elements.discipline = evaluation
            binding.btnEvaluate.run {
                show(object : ExtendedFloatingActionButton.OnChangedListener() {
                    override fun onShown(extendedFab: ExtendedFloatingActionButton?) {
                        super.onShown(extendedFab)
                        extend(true)
                    }
                })
            }
        }
        when (resource.status) {
            Status.ERROR -> {
                binding.itemsRecycler.visibility = GONE
                binding.loadingGroup.visibility = GONE
                binding.failedGroup.visibility = VISIBLE
            }
            Status.LOADING -> {
                binding.itemsRecycler.visibility = GONE
                binding.loadingGroup.visibility = VISIBLE
                binding.failedGroup.visibility = GONE
            }
            Status.SUCCESS -> {
                binding.itemsRecycler.visibility = VISIBLE
                binding.loadingGroup.visibility = GONE
                binding.failedGroup.visibility = GONE
            }
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
    val id: Long,
    val name: String,
    val mean: Double
)

data class TeacherInt(
    val id: Long,
    val name: String,
    val lastSeen: String,
    val mean: Double
)