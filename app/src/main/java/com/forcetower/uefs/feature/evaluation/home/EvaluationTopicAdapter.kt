package com.forcetower.uefs.feature.evaluation.home

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.EvaluationDiscipline
import com.forcetower.uefs.core.model.service.EvaluationHomeTopic
import com.forcetower.uefs.core.model.service.EvaluationTeacher
import com.forcetower.uefs.databinding.ItemEvaluateDisciplineHomeBinding
import com.forcetower.uefs.databinding.ItemEvaluateTeacherHomeBinding
import com.forcetower.uefs.databinding.ItemEvaluationHeaderBinding
import com.forcetower.uefs.feature.shared.inflate

class EvaluationTopicAdapter(
    private val interactor: HomeInteractor
) : RecyclerView.Adapter<EvaluationHolder>() {
    private val differ = AsyncListDiffer<Any>(this, DiffCallback)

    var currentList: List<EvaluationHomeTopic> = listOf()
        set(value) {
            field = value
            differ.submitList(buildMergedList(value))
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvaluationHolder {
        return when (viewType) {
            R.layout.item_evaluation_header -> EvaluationHolder.EvaluationHeader(parent.inflate(viewType))
            R.layout.item_evaluate_discipline_home -> EvaluationHolder.EvaluationDiscipline(parent.inflate(viewType), interactor)
            R.layout.item_evaluate_teacher_home -> EvaluationHolder.EvaluationTeacher(parent.inflate(viewType))
            else -> throw IllegalStateException("Unable to inflate $viewType")
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun onBindViewHolder(holder: EvaluationHolder, position: Int) {
        when (holder) {
            is EvaluationHolder.EvaluationHeader -> {
                val topic = differ.currentList[position] as EvaluationHomeTopic
                holder.binding.apply {
                    header = topic
                    executePendingBindings()
                }
            }
            is EvaluationHolder.EvaluationTeacher -> {
                val wrap = differ.currentList[position] as TeacherWrapper
                holder.binding.apply {
                    teacher = wrap.teacher
                    executePendingBindings()
                }
            }
            is EvaluationHolder.EvaluationDiscipline -> {
                val wrap = differ.currentList[position] as DisciplineWrapper
                holder.binding.apply {
                    discipline = wrap.discipline
                    executePendingBindings()
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            is EvaluationHomeTopic -> R.layout.item_evaluation_header
            is DisciplineWrapper -> R.layout.item_evaluate_discipline_home
            is TeacherWrapper -> R.layout.item_evaluate_teacher_home
            else -> throw IllegalStateException("Can't find view for object at position $position ${differ.currentList[position]}")
        }
    }

    private fun buildMergedList(list: List<EvaluationHomeTopic>?): List<Any> {
        val result = mutableListOf<Any>()
        list?.forEach {
            result += it
            val teachers = it.teachers
            val disciplines = it.disciplines
            if (teachers != null) result.addAll(teachers.map { value -> TeacherWrapper(value, it.id) })
            if (disciplines != null) result.addAll(disciplines.map { value -> DisciplineWrapper(value, it.id) })
        }
        return result
    }
}

private data class TeacherWrapper(val teacher: EvaluationTeacher, val groupId: Int)
private data class DisciplineWrapper(val discipline: EvaluationDiscipline, val groupId: Int)

sealed class EvaluationHolder(view: View) : RecyclerView.ViewHolder(view) {
    class EvaluationHeader(val binding: ItemEvaluationHeaderBinding) : EvaluationHolder(binding.root)
    class EvaluationDiscipline(val binding: ItemEvaluateDisciplineHomeBinding, interactor: HomeInteractor) : EvaluationHolder(binding.root) {
        init { binding.interactor = interactor }
    }
    class EvaluationTeacher(val binding: ItemEvaluateTeacherHomeBinding) : EvaluationHolder(binding.root)
}

private object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is EvaluationHomeTopic && newItem is EvaluationHomeTopic -> {
                oldItem.id == newItem.id
            }
            oldItem is TeacherWrapper && newItem is TeacherWrapper -> {
                oldItem.groupId == newItem.groupId &&
                        oldItem.teacher.teacherId == newItem.teacher.teacherId
            }
            oldItem is DisciplineWrapper && newItem is DisciplineWrapper -> {
                oldItem.groupId == newItem.groupId &&
                        oldItem.discipline.disciplineId == newItem.discipline.disciplineId
            }
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is EvaluationHomeTopic && newItem is EvaluationHomeTopic -> {
                oldItem.title == newItem.title && oldItem.description == newItem.description
            }
            oldItem is TeacherWrapper && newItem is TeacherWrapper -> {
                oldItem.teacher == newItem.teacher
            }
            oldItem is DisciplineWrapper && newItem is DisciplineWrapper -> {
                oldItem.discipline == newItem.discipline
            }
            else -> true
        }
    }
}
