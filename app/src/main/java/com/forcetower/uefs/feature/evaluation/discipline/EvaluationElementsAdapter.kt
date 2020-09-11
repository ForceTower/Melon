/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.feature.evaluation.discipline

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ItemEvaluationDisciplineHeaderBinding
import com.forcetower.uefs.databinding.ItemEvaluationMeanBinding
import com.forcetower.uefs.databinding.ItemEvaluationTeacherMiniBinding
import com.forcetower.uefs.databinding.ItemEvaluationTeachersHeaderBinding
import com.forcetower.uefs.feature.shared.inflate

class EvaluationElementsAdapter(
    private val interactor: DisciplineInteractor
) : RecyclerView.Adapter<ElementHolder>() {
    var discipline: DisciplineEvaluation? = null
        set(value) {
            field = value
            diff.submitList(buildList(value))
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElementHolder {
        return when (viewType) {
            R.layout.item_evaluation_mean -> ElementHolder.MeanHolder(parent.inflate(viewType))
            R.layout.item_evaluation_teachers_header -> ElementHolder.TeacherHeader(parent.inflate(viewType))
            R.layout.item_evaluation_teacher_mini -> ElementHolder.TeacherHolder(parent.inflate(viewType), interactor)
            R.layout.item_evaluation_discipline_header -> ElementHolder.DisciplineHeader(parent.inflate(viewType))
            else -> throw IllegalStateException("No view defined for type $viewType")
        }
    }

    override fun getItemCount() = diff.currentList.size

    override fun onBindViewHolder(holder: ElementHolder, position: Int) {
        val item = diff.currentList[position]
        when (holder) {
            is ElementHolder.MeanHolder -> holder.bind((item as Mean).list, discipline)
            is ElementHolder.TeacherHolder -> holder.view.teacher = item as TeacherInt
            is ElementHolder.DisciplineHeader -> holder.view.discipline = item as DisciplineEvaluation
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (diff.currentList[position]) {
            is Mean -> R.layout.item_evaluation_mean
            is Teacher -> R.layout.item_evaluation_teachers_header
            is TeacherInt -> R.layout.item_evaluation_teacher_mini
            is DisciplineEvaluation -> R.layout.item_evaluation_discipline_header
            else -> throw IllegalStateException("No view type defined for position $position")
        }
    }

    private val diff = AsyncListDiffer(this, DiffCallback)

    private fun buildList(value: DisciplineEvaluation?): List<Any> {
        val list = mutableListOf<Any>()
        value ?: return list
        list += value
        list += Mean(value.grades)
        if (value.teachers.isNotEmpty()) {
            list += Teacher
            list.addAll(value.teachers)
        }
        return list
    }
}

sealed class ElementHolder(view: View) : RecyclerView.ViewHolder(view) {
    class MeanHolder(val view: ItemEvaluationMeanBinding) : ElementHolder(view.root) {
        fun bind(grades: List<SemesterMean>, discipline: DisciplineEvaluation? = null) {
            view.grades = grades
            discipline ?: return
            view.usersAmount.text = view.root.context.getString(R.string.evaluation_mean_amount, discipline.amount)
        }
    }
    class TeacherHeader(val view: ItemEvaluationTeachersHeaderBinding) : ElementHolder(view.root)
    class TeacherHolder(val view: ItemEvaluationTeacherMiniBinding, interactor: DisciplineInteractor) : ElementHolder(view.root) {
        init {
            view.interactor = interactor
        }
    }
    class DisciplineHeader(val view: ItemEvaluationDisciplineHeaderBinding) : ElementHolder(view.root)
}

// TODO create this
private object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return false
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return true
    }
}

private object Header
private data class Mean(val list: List<SemesterMean>)
private object Teacher
