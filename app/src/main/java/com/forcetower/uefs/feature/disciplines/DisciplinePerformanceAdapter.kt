/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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
import com.forcetower.uefs.core.storage.database.accessors.ClassWithGroups
import com.forcetower.uefs.databinding.ItemDisciplineStatusDividerBinding
import com.forcetower.uefs.databinding.ItemDisciplineStatusFinalsBinding
import com.forcetower.uefs.databinding.ItemDisciplineStatusMeanBinding
import com.forcetower.uefs.databinding.ItemDisciplineStatusNameResumedBinding
import com.forcetower.uefs.databinding.ItemGradeBinding
import com.forcetower.uefs.feature.common.DisciplineActions
import com.forcetower.uefs.feature.shared.inflate

class DisciplinePerformanceAdapter(
    private val viewModel: DisciplineViewModel
) : RecyclerView.Adapter<DisciplinePerformanceAdapter.DisciplineHolder>() {
    private val differ = AsyncListDiffer(this, DiffCallback)
    var classes: List<ClassWithGroups> = emptyList()
        set(value) {
            field = value
            differ.submitList(buildMergedList(classes))
        }

    private fun buildMergedList(classes: List<ClassWithGroups>): List<Any> {
        val list = mutableListOf<Any>()
        classes.sortedBy { it.discipline().name }.forEachIndexed { index, clazz ->
            if (index != 0)
                list += Divider

            list += Header(clazz)
            clazz.grades.sortedBy { it.name }.forEach { grade ->
                list += Score(clazz, grade)
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
            R.layout.item_discipline_status_name_resumed -> DisciplineHolder.HeaderHolder(parent.inflate(viewType), viewModel)
            R.layout.item_grade -> DisciplineHolder.GradeHolder(parent.inflate(viewType), viewModel)
            R.layout.item_discipline_status_finals -> DisciplineHolder.FinalsHolder(parent.inflate(viewType), viewModel)
            R.layout.item_discipline_status_mean -> DisciplineHolder.MeanHolder(parent.inflate(viewType), viewModel)
            R.layout.item_discipline_status_divider -> DisciplineHolder.DividerHolder(parent.inflate(viewType))
            else -> throw IllegalStateException("No view defined for $viewType")
        }
    }

    override fun onBindViewHolder(holder: DisciplineHolder, position: Int) {
        val item = differ.currentList[position]
        when (holder) {
            is DisciplineHolder.HeaderHolder -> {
                holder.binding.apply {
                    clazzGroup = (item as Header).clazz
                    executePendingBindings()
                }
            }
            is DisciplineHolder.GradeHolder -> {
                holder.binding.apply {
                    val element = item as Score
                    clazzGroup = element.clazz
                    grade = element.grade
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
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return when (val item = differ.currentList[position]) {
            is Header -> R.layout.item_discipline_status_name_resumed
            is Score -> R.layout.item_grade
            is Final -> R.layout.item_discipline_status_finals
            is Mean -> R.layout.item_discipline_status_mean
            is Divider -> R.layout.item_discipline_status_divider
            else -> throw IllegalStateException("No view type defined for $item")
        }
    }

    sealed class DisciplineHolder(view: View) : RecyclerView.ViewHolder(view) {
        class HeaderHolder(
            val binding: ItemDisciplineStatusNameResumedBinding,
            listener: DisciplineActions
        ) : DisciplineHolder(binding.root) {
            init { binding.listener = listener }
        }

        class GradeHolder(
            val binding: ItemGradeBinding,
            listener: DisciplineActions
        ) : DisciplineHolder(binding.root) {
            init { binding.listener = listener }
        }

        class FinalsHolder(
            val binding: ItemDisciplineStatusFinalsBinding,
            listener: DisciplineActions
        ) : DisciplineHolder(binding.root) {
            init { binding.listener = listener }
        }

        class MeanHolder(
            val binding: ItemDisciplineStatusMeanBinding,
            listener: DisciplineActions
        ) : DisciplineHolder(binding.root) {
            init { binding.listener = listener }
        }

        class DividerHolder(
            val binding: ItemDisciplineStatusDividerBinding
        ) : DisciplineHolder(binding.root)
    }

    private object DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Header && newItem is Header -> newItem.clazz.discipline().uid == oldItem.clazz.discipline().uid
                oldItem is Final && newItem is Final -> newItem.clazz.clazz.uid == oldItem.clazz.clazz.uid
                oldItem is Mean && newItem is Mean -> newItem.clazz.clazz.uid == oldItem.clazz.clazz.uid
                oldItem is Score && newItem is Score -> newItem.grade.uid == oldItem.grade.uid
                oldItem is Divider && newItem is Divider -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Header && newItem is Header -> newItem.clazz.discipline() == oldItem.clazz.discipline()
                oldItem is Final && newItem is Final -> newItem.clazz.clazz == oldItem.clazz.clazz
                oldItem is Mean && newItem is Mean -> newItem.clazz.clazz == oldItem.clazz.clazz
                oldItem is Score && newItem is Score -> newItem.grade == oldItem.grade
                else -> true
            }
        }
    }

    private data class Header(val clazz: ClassWithGroups)
    private data class Score(val clazz: ClassWithGroups, val grade: Grade)
    private data class Final(val clazz: ClassWithGroups)
    private data class Mean(val clazz: ClassWithGroups)
    private object Divider
}