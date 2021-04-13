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

package dev.forcetower.disciplines.feature

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.ui.disciplines.DisciplineHelperData
import com.forcetower.uefs.feature.shared.inflate
import dev.forcetower.disciplines.R
import dev.forcetower.disciplines.databinding.ItemDisciplineEmptyDataBinding
import dev.forcetower.disciplines.databinding.ItemDisciplineStatusDividerBinding
import dev.forcetower.disciplines.databinding.ItemDisciplineStatusFinalsBinding
import dev.forcetower.disciplines.databinding.ItemDisciplineStatusGroupingNameBinding
import dev.forcetower.disciplines.databinding.ItemDisciplineStatusMeanBinding
import dev.forcetower.disciplines.databinding.ItemDisciplineStatusNameResumedBinding
import dev.forcetower.disciplines.databinding.ItemGradeBinding

class DisciplinePerformanceAdapter(
    private val actions: DisciplinesSemestersActions
) : ListAdapter<DisciplineHelperData, DisciplinePerformanceAdapter.DisciplineHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisciplineHolder {
        return when (viewType) {
            R.layout.item_discipline_status_name_resumed -> DisciplineHolder.HeaderHolder(parent.inflate(viewType), actions)
            R.layout.item_grade -> DisciplineHolder.GradeHolder(parent.inflate(viewType), actions)
            R.layout.item_discipline_status_finals -> DisciplineHolder.FinalsHolder(parent.inflate(viewType), actions)
            R.layout.item_discipline_status_mean -> DisciplineHolder.MeanHolder(parent.inflate(viewType), actions)
            R.layout.item_discipline_status_divider -> DisciplineHolder.DividerHolder(parent.inflate(viewType))
            R.layout.item_discipline_status_grouping_name -> DisciplineHolder.GroupingHolder(parent.inflate(viewType), actions)
            R.layout.item_discipline_empty_data -> DisciplineHolder.EmptySemester(parent.inflate(viewType), actions)
            else -> throw IllegalStateException("No view defined for $viewType")
        }
    }

    override fun onBindViewHolder(holder: DisciplineHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is DisciplineHolder.GradeHolder -> {
                holder.binding.apply {
                    val element = item as DisciplineHelperData.Score
                    clazzGroup = element.clazz
                    grade = element.grade
                    executePendingBindings()
                }
            }
            is DisciplineHolder.HeaderHolder -> {
                holder.binding.apply {
                    clazzGroup = (item as DisciplineHelperData.Header).clazz
                    executePendingBindings()
                }
            }
            is DisciplineHolder.FinalsHolder -> {
                holder.binding.apply {
                    clazzGroup = (item as DisciplineHelperData.Final).clazz
                    executePendingBindings()
                }
            }
            is DisciplineHolder.MeanHolder -> {
                holder.binding.apply {
                    clazzGroup = (item as DisciplineHelperData.Mean).clazz
                    executePendingBindings()
                }
            }
            is DisciplineHolder.GroupingHolder -> {
                holder.binding.apply {
                    val element = item as DisciplineHelperData.GroupingName
                    clazzGroup = element.clazz
                    groupingName = element.name
                    executePendingBindings()
                }
            }
            is DisciplineHolder.EmptySemester -> {
                holder.binding.apply {
                    val element = item as DisciplineHelperData.EmptySemester
                    semester = element.semester
                    executePendingBindings()
                }
            }
            else -> Unit
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is DisciplineHelperData.Header -> R.layout.item_discipline_status_name_resumed
            is DisciplineHelperData.Score -> R.layout.item_grade
            is DisciplineHelperData.Final -> R.layout.item_discipline_status_finals
            is DisciplineHelperData.Mean -> R.layout.item_discipline_status_mean
            is DisciplineHelperData.Divider -> R.layout.item_discipline_status_divider
            is DisciplineHelperData.GroupingName -> R.layout.item_discipline_status_grouping_name
            is DisciplineHelperData.EmptySemester -> R.layout.item_discipline_empty_data
            else -> throw IllegalStateException("No view type defined for $item")
        }
    }

    sealed class DisciplineHolder(view: View) : RecyclerView.ViewHolder(view) {
        class GroupingHolder(
            val binding: ItemDisciplineStatusGroupingNameBinding,
            listener: DisciplinesSemestersActions
        ) : DisciplineHolder(binding.root) {
            init { binding.actions = listener }
        }

        class MeanHolder(
            val binding: ItemDisciplineStatusMeanBinding,
            listener: DisciplinesSemestersActions
        ) : DisciplineHolder(binding.root) {
            init { binding.actions = listener }
        }

        class FinalsHolder(
            val binding: ItemDisciplineStatusFinalsBinding,
            listener: DisciplinesSemestersActions
        ) : DisciplineHolder(binding.root) {
            init { binding.actions = listener }
        }

        class GradeHolder(
            val binding: ItemGradeBinding,
            listener: DisciplinesSemestersActions
        ) : DisciplineHolder(binding.root) {
            init { binding.actions = listener }
        }

        class HeaderHolder(
            val binding: ItemDisciplineStatusNameResumedBinding,
            listener: DisciplinesSemestersActions
        ) : DisciplineHolder(binding.root) {
            init { binding.actions = listener }
        }

        class DividerHolder(
            binding: ItemDisciplineStatusDividerBinding
        ) : DisciplineHolder(binding.root)

        class EmptySemester(
            val binding: ItemDisciplineEmptyDataBinding,
            actions: DisciplinesSemestersActions
        ) : DisciplineHolder(binding.root) {
            init { binding.actions = actions }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DisciplineHelperData>() {
        override fun areItemsTheSame(oldItem: DisciplineHelperData, newItem: DisciplineHelperData): Boolean {
            return when {
                oldItem is DisciplineHelperData.Header && newItem is DisciplineHelperData.Header -> newItem.clazz.discipline.uid == oldItem.clazz.discipline.uid
                oldItem is DisciplineHelperData.Final && newItem is DisciplineHelperData.Final -> newItem.clazz.clazz.uid == oldItem.clazz.clazz.uid
                oldItem is DisciplineHelperData.Mean && newItem is DisciplineHelperData.Mean -> newItem.clazz.clazz.uid == oldItem.clazz.clazz.uid
                oldItem is DisciplineHelperData.Score && newItem is DisciplineHelperData.Score -> newItem.grade.uid == oldItem.grade.uid
                oldItem is DisciplineHelperData.Divider && newItem is DisciplineHelperData.Divider -> true
                oldItem is DisciplineHelperData.GroupingName && newItem is DisciplineHelperData.GroupingName -> newItem.name == oldItem.name
                oldItem is DisciplineHelperData.EmptySemester && newItem is DisciplineHelperData.EmptySemester -> oldItem.semester.uid == newItem.semester.uid
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: DisciplineHelperData, newItem: DisciplineHelperData): Boolean {
            return when {
                oldItem is DisciplineHelperData.Header && newItem is DisciplineHelperData.Header -> newItem.clazz.discipline == oldItem.clazz.discipline
                oldItem is DisciplineHelperData.Final && newItem is DisciplineHelperData.Final -> newItem.clazz == oldItem.clazz
                oldItem is DisciplineHelperData.Mean && newItem is DisciplineHelperData.Mean -> newItem.clazz == oldItem.clazz
                oldItem is DisciplineHelperData.Score && newItem is DisciplineHelperData.Score -> newItem.grade == oldItem.grade
                oldItem is DisciplineHelperData.GroupingName && newItem is DisciplineHelperData.GroupingName -> newItem.name == oldItem.name
                oldItem is DisciplineHelperData.EmptySemester && newItem is DisciplineHelperData.EmptySemester -> oldItem.semester == newItem.semester
                else -> true
            }
        }
    }
}
