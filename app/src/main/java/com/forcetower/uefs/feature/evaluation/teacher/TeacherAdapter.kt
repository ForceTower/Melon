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

package com.forcetower.uefs.feature.evaluation.teacher

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.EvaluationDiscipline
import com.forcetower.uefs.core.model.service.EvaluationTeacher
import com.forcetower.uefs.databinding.ItemEvaluateDisciplineHomeBinding
import com.forcetower.uefs.databinding.ItemEvaluationTeacherGraphicsBinding
import com.forcetower.uefs.databinding.ItemEvaluationTeacherMeanBinding
import com.forcetower.uefs.databinding.ItemEvaluationTeacherOffersHeaderBinding
import com.forcetower.uefs.feature.evaluation.home.HomeInteractor
import com.forcetower.uefs.feature.shared.inflate

class TeacherAdapter(
    private val interactor: HomeInteractor
) : RecyclerView.Adapter<TeacherHolder>() {
    var discipline: EvaluationTeacher? = null
        set(value) {
            field = value
            diff.submitList(buildList(value))
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherHolder {
        return when (viewType) {
            R.layout.item_evaluation_teacher_mean -> TeacherHolder.GeneralMeanHolder(parent.inflate(viewType))
            R.layout.item_evaluation_teacher_graphics -> TeacherHolder.StatisticsApprovalHolder(parent.inflate(viewType))
            R.layout.item_evaluation_teacher_offers_header -> TeacherHolder.DisciplineHeaderHolder(parent.inflate(viewType))
            R.layout.item_evaluate_discipline_home -> TeacherHolder.DisciplineHolder(parent.inflate(viewType), interactor)
            else -> throw IllegalStateException("No view holder found for view type $viewType")
        }
    }

    override fun getItemCount() = diff.currentList.size

    override fun onBindViewHolder(holder: TeacherHolder, position: Int) {
        val item = diff.currentList[position]
        when (holder) {
            is TeacherHolder.GeneralMeanHolder -> {
                holder.binding.apply {
                    teacher = (item as GeneralMeanWrapper).data
                    executePendingBindings()
                }
            }
            is TeacherHolder.StatisticsApprovalHolder -> {
                holder.binding.apply {
                    val cast = (item as StatisticsApproval).data
                    amountApprovals = cast.approved
                    amountFails = cast.failed
                    amountFinals = cast.finals
                    total = cast.qtdStudents
                    executePendingBindings()
                }
            }
            is TeacherHolder.DisciplineHolder -> {
                holder.binding.apply {
                    val value = item as EvaluationDiscipline
                    discipline = value
                    executePendingBindings()
                }
            }
        }
    }

    private val diff = AsyncListDiffer(this, DiffCallback)

    private fun buildList(value: EvaluationTeacher?): List<Any> {
        val list = mutableListOf<Any>()
        value ?: return list
        if (value.imageUrl != null) {
            list += GeneralMeanWrapper(value)
        }
        list += StatisticsApproval(value)

        val disciplines = value.disciplines
        if (disciplines?.isNotEmpty() == true) {
            list += DisciplineHeader
            list.addAll(value.disciplines)
        }
        return list
    }

    override fun getItemViewType(position: Int): Int {
        return when (diff.currentList[position]) {
            is GeneralMeanWrapper -> R.layout.item_evaluation_teacher_mean
            is StatisticsApproval -> R.layout.item_evaluation_teacher_graphics
            is DisciplineHeader -> R.layout.item_evaluation_teacher_offers_header
            is EvaluationDiscipline -> R.layout.item_evaluate_discipline_home
            else -> throw IllegalStateException("No view found for position $position -> object is: ${diff.currentList[position]}")
        }
    }
}

// TODO Create this
private object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return false
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return true
    }
}

sealed class TeacherHolder(view: View) : RecyclerView.ViewHolder(view) {
    class GeneralMeanHolder(val binding: ItemEvaluationTeacherMeanBinding) : TeacherHolder(binding.root)
    class StatisticsApprovalHolder(val binding: ItemEvaluationTeacherGraphicsBinding) : TeacherHolder(binding.root)
    class DisciplineHeaderHolder(val binding: ItemEvaluationTeacherOffersHeaderBinding) : TeacherHolder(binding.root)
    class DisciplineHolder(val binding: ItemEvaluateDisciplineHomeBinding, val interactor: HomeInteractor) : TeacherHolder(binding.root) {
        init {
            binding.interactor = interactor
        }
    }
}

private data class GeneralMeanWrapper(val data: EvaluationTeacher)
private data class StatisticsApproval(val data: EvaluationTeacher)
private object DisciplineHeader
