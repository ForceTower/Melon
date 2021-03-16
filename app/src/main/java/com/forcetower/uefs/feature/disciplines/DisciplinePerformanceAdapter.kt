/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.feature.disciplines

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup
import com.forcetower.uefs.databinding.ItemDisciplineStatusDividerOldBinding
import com.forcetower.uefs.databinding.ItemDisciplineStatusFinalsOldBinding
import com.forcetower.uefs.databinding.ItemDisciplineStatusGroupingNameOldBinding
import com.forcetower.uefs.databinding.ItemDisciplineStatusMeanOldBinding
import com.forcetower.uefs.databinding.ItemDisciplineStatusNameResumedOldBinding
import com.forcetower.uefs.databinding.ItemGradeOldBinding
import com.forcetower.uefs.feature.common.DisciplineActions
import com.forcetower.uefs.feature.shared.inflate

class DisciplinePerformanceAdapter(
    private val viewModel: DisciplineViewModel
) : RecyclerView.Adapter<DisciplinePerformanceAdapter.DisciplineHolder>() {
    private val differ = AsyncListDiffer(this, DiffCallback)
    var classes: List<ClassFullWithGroup> = emptyList()
        set(value) {
            field = value
            differ.submitList(buildMergedList(classes))
        }

    private fun buildMergedList(classes: List<ClassFullWithGroup>): List<Any> {
        val list = mutableListOf<Any>()
        classes.sortedBy { it.discipline.name }.forEachIndexed { index, clazz ->
            if (index != 0)
                list += Divider

            list += Header(clazz)

            val groupings = clazz.grades.groupBy { it.grouping }
            if (groupings.keys.size <= 1) {
                clazz.grades.sortedBy { it.name }.forEach { grade ->
                    list += Score(clazz, grade)
                }
            } else {
                groupings.entries.sortedBy { it.key }.forEach { (_, value) ->
                    if (value.isNotEmpty()) {
                        val sample = value[0]
                        list += GroupingName(clazz, sample.groupingName)
                        value.sortedBy { it.name }.forEach { grade ->
                            list += Score(clazz, grade)
                        }
                    }
                }
            }

            if (clazz.clazz.isInFinal()) {
                list += Final(clazz)
            }
            list += Mean(clazz)
        }
        return list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisciplineHolder {
        return when (viewType) {
            R.layout.item_discipline_status_name_resumed_old -> DisciplineHolder.HeaderHolder(parent.inflate(viewType), viewModel)
            R.layout.item_grade_old -> DisciplineHolder.GradeHolder(parent.inflate(viewType), viewModel)
            R.layout.item_discipline_status_finals_old -> DisciplineHolder.FinalsHolder(parent.inflate(viewType), viewModel)
            R.layout.item_discipline_status_mean_old -> DisciplineHolder.MeanHolder(parent.inflate(viewType), viewModel)
            R.layout.item_discipline_status_divider_old -> DisciplineHolder.DividerHolder(parent.inflate(viewType))
            R.layout.item_discipline_status_grouping_name_old -> DisciplineHolder.GroupingHolder(parent.inflate(viewType), viewModel)
            else -> throw IllegalStateException("No view defined for $viewType")
        }
    }

    override fun onBindViewHolder(holder: DisciplineHolder, position: Int) {
        val item = differ.currentList[position]
        when (holder) {
            is DisciplineHolder.GradeHolder -> {
                holder.binding.apply {
                    val element = item as Score
                    clazzGroup = element.clazz
                    grade = element.grade
                    executePendingBindings()
                }
            }
            is DisciplineHolder.HeaderHolder -> {
                holder.binding.apply {
                    clazzGroup = (item as Header).clazz
                    executePendingBindings()
                }
            }
            is DisciplineHolder.FinalsHolder -> {
                holder.binding.apply {
                    clazzGroup = (item as Final).clazz
                    executePendingBindings()
                }
            }
            is DisciplineHolder.MeanHolder -> {
                holder.binding.apply {
                    clazzGroup = (item as Mean).clazz
                    executePendingBindings()
                }
            }
            is DisciplineHolder.GroupingHolder -> {
                holder.binding.apply {
                    val element = item as GroupingName
                    clazzGroup = element.clazz
                    groupingName = element.name
                    executePendingBindings()
                }
            }
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return when (val item = differ.currentList[position]) {
            is Header -> R.layout.item_discipline_status_name_resumed_old
            is Score -> R.layout.item_grade_old
            is Final -> R.layout.item_discipline_status_finals_old
            is Mean -> R.layout.item_discipline_status_mean_old
            is Divider -> R.layout.item_discipline_status_divider_old
            is GroupingName -> R.layout.item_discipline_status_grouping_name_old
            else -> throw IllegalStateException("No view type defined for $item")
        }
    }

    sealed class DisciplineHolder(view: View) : RecyclerView.ViewHolder(view) {
        class GroupingHolder(
            val binding: ItemDisciplineStatusGroupingNameOldBinding,
            listener: DisciplineActions
        ) : DisciplineHolder(binding.root) {
            init { binding.listener = listener }
        }

        class MeanHolder(
            val binding: ItemDisciplineStatusMeanOldBinding,
            listener: DisciplineActions
        ) : DisciplineHolder(binding.root) {
            init { binding.listener = listener }
        }

        class FinalsHolder(
            val binding: ItemDisciplineStatusFinalsOldBinding,
            listener: DisciplineActions
        ) : DisciplineHolder(binding.root) {
            init { binding.listener = listener }
        }

        class GradeHolder(
            val binding: ItemGradeOldBinding,
            listener: DisciplineActions
        ) : DisciplineHolder(binding.root) {
            init { binding.listener = listener }
        }

        class HeaderHolder(
            val binding: ItemDisciplineStatusNameResumedOldBinding,
            listener: DisciplineActions
        ) : DisciplineHolder(binding.root) {
            init { binding.listener = listener }
        }

        class DividerHolder(
            val binding: ItemDisciplineStatusDividerOldBinding
        ) : DisciplineHolder(binding.root)
    }

    private object DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Header && newItem is Header -> newItem.clazz.discipline.uid == oldItem.clazz.discipline.uid
                oldItem is Final && newItem is Final -> newItem.clazz.clazz.uid == oldItem.clazz.clazz.uid
                oldItem is Mean && newItem is Mean -> newItem.clazz.clazz.uid == oldItem.clazz.clazz.uid
                oldItem is Score && newItem is Score -> newItem.grade.uid == oldItem.grade.uid
                oldItem is Divider && newItem is Divider -> true
                oldItem is GroupingName && newItem is GroupingName -> newItem.name == oldItem.name
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Header && newItem is Header -> newItem.clazz.discipline == oldItem.clazz.discipline
                oldItem is Final && newItem is Final -> newItem.clazz == oldItem.clazz
                oldItem is Mean && newItem is Mean -> newItem.clazz == oldItem.clazz
                oldItem is Score && newItem is Score -> newItem.grade == oldItem.grade
                oldItem is GroupingName && newItem is GroupingName -> newItem.name == oldItem.name
                else -> true
            }
        }
    }

    private data class Header(val clazz: ClassFullWithGroup)
    private data class Score(val clazz: ClassFullWithGroup, val grade: Grade)
    private data class Final(val clazz: ClassFullWithGroup)
    private data class Mean(val clazz: ClassFullWithGroup)
    private data class GroupingName(val clazz: ClassFullWithGroup, val name: String)
    private object Divider
}
