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

package com.forcetower.uefs.feature.flowchart.discipline

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.core.model.unes.FlowchartRequirementUI
import com.forcetower.uefs.databinding.ItemFlowchartDisciplineDetailsHeaderBinding
import com.forcetower.uefs.databinding.ItemFlowchartDisciplineGroupingBinding
import com.forcetower.uefs.databinding.ItemFlowchartDisciplineMinifiedBinding
import com.forcetower.uefs.databinding.ItemFlowchartDisciplineProgramBinding
import com.forcetower.uefs.feature.flowchart.semester.DisciplineInteractor
import com.forcetower.uefs.feature.shared.inflate
import timber.log.Timber

class DisciplineDetailsAdapter(
    private val interactor: DisciplineInteractor
) : RecyclerView.Adapter<DisciplineDetailsAdapter.DisciplineDetailsHolder>() {
    private val differ = AsyncListDiffer(this, DiffCallback)
    var currentList: List<FlowchartRequirementUI> = listOf()
        set(value) {
            field = value
            differ.submitList(buildMergedList(default = value))
        }
    var discipline: FlowchartDisciplineUI? = null
        set(value) {
            field = value
            differ.submitList(buildMergedList(item = value))
        }

    var semesterValue: String? = null
        set(value) {
            field = value
            differ.submitList(buildMergedList(semester = value))
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisciplineDetailsHolder {
        return when (viewType) {
            R.layout.item_flowchart_discipline_grouping -> DisciplineDetailsHolder.CategoryHolder(parent.inflate(viewType))
            R.layout.item_flowchart_discipline_minified -> DisciplineDetailsHolder.DisciplineHolder(parent.inflate(viewType), interactor)
            R.layout.item_flowchart_discipline_program -> DisciplineDetailsHolder.ResumeHolder(parent.inflate(viewType))
            R.layout.item_flowchart_discipline_details_header -> DisciplineDetailsHolder.HeaderHolder(parent.inflate(viewType))
            else -> throw IllegalStateException("No view defined for view type $viewType")
        }
    }

    override fun getItemCount(): Int {
        Timber.d("Calculated new size ${differ.currentList.size}")
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: DisciplineDetailsHolder, position: Int) {
        val item = differ.currentList[position]
        when (holder) {
            is DisciplineDetailsHolder.HeaderHolder -> {
                holder.binding.apply {
                    val casted = (item as Header)
                    discipline = casted.discipline
                    semesterValue = casted.semester
                    executePendingBindings()
                }
            }
            is DisciplineDetailsHolder.ResumeHolder -> {
                holder.binding.apply {
                    discipline = (item as Resume).discipline
                    executePendingBindings()
                }
            }
            is DisciplineDetailsHolder.CategoryHolder -> {
                holder.binding.apply {
                    name = (item as DisciplineTitle).title
                    executePendingBindings()
                }
            }
            is DisciplineDetailsHolder.DisciplineHolder -> {
                holder.binding.apply {
                    requirement = item as FlowchartRequirementUI
                    executePendingBindings()
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = differ.currentList[position]) {
            is Header -> R.layout.item_flowchart_discipline_details_header
            is Resume -> R.layout.item_flowchart_discipline_program
            is DisciplineTitle -> R.layout.item_flowchart_discipline_grouping
            is FlowchartRequirementUI -> R.layout.item_flowchart_discipline_minified
            else -> throw IllegalStateException("No view defined for position $position item $item")
        }
    }

    private fun buildMergedList(
        default: List<FlowchartRequirementUI> = currentList,
        item: FlowchartDisciplineUI? = discipline,
        semester: String? = semesterValue
    ): List<Any> {
        val result = mutableListOf<Any>()

        if (item != null) {
            result += Header(item, semester)
            if (item.program != null)
                result += Resume(item)
        }

        val mapped = default.groupBy { it.type }
        mapped.entries.forEach {
            result += DisciplineTitle(it.key)
            result.addAll(it.value)
        }
        return result
    }

    private data class DisciplineTitle(
        val title: String
    )

    sealed class DisciplineDetailsHolder(view: View) : RecyclerView.ViewHolder(view) {
        class HeaderHolder(val binding: ItemFlowchartDisciplineDetailsHeaderBinding) : DisciplineDetailsHolder(binding.root)
        class ResumeHolder(val binding: ItemFlowchartDisciplineProgramBinding) : DisciplineDetailsHolder(binding.root)
        class CategoryHolder(val binding: ItemFlowchartDisciplineGroupingBinding) : DisciplineDetailsHolder(binding.root)
        class DisciplineHolder(val binding: ItemFlowchartDisciplineMinifiedBinding, interactor: DisciplineInteractor) : DisciplineDetailsHolder(binding.root) {
            init { binding.interactor = interactor }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Header && newItem is Header -> oldItem.discipline.id == newItem.discipline.id
                oldItem is Resume && newItem is Resume -> oldItem.discipline.id == newItem.discipline.id
                oldItem is DisciplineTitle && newItem is DisciplineTitle -> oldItem == newItem
                oldItem is FlowchartRequirementUI && newItem is FlowchartRequirementUI -> oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Header && newItem is Header -> oldItem == newItem
                oldItem is Resume && newItem is Resume -> oldItem == newItem
                oldItem is DisciplineTitle && newItem is DisciplineTitle -> oldItem == newItem
                oldItem is FlowchartRequirementUI && newItem is FlowchartRequirementUI -> oldItem == newItem
                else -> true
            }
        }
    }

    private data class Header(val discipline: FlowchartDisciplineUI, val semester: String?)
    private data class Resume(val discipline: FlowchartDisciplineUI)
}
