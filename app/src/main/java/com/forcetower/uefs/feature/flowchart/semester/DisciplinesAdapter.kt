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

package com.forcetower.uefs.feature.flowchart.semester

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.databinding.ItemFlowchartDisciplineBinding
import com.forcetower.uefs.feature.shared.inflate

class DisciplinesAdapter(
    private val interactor: DisciplineInteractor
) : ListAdapter<FlowchartDisciplineUI, DisciplinesAdapter.DisciplineHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisciplineHolder {
        return DisciplineHolder(parent.inflate(R.layout.item_flowchart_discipline), interactor)
    }

    override fun onBindViewHolder(holder: DisciplineHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            discipline = item
            executePendingBindings()
        }
    }

    class DisciplineHolder(val binding: ItemFlowchartDisciplineBinding, interactor: DisciplineInteractor) : RecyclerView.ViewHolder(binding.root) {
        init { binding.interactor = interactor }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FlowchartDisciplineUI>() {
        override fun areItemsTheSame(oldItem: FlowchartDisciplineUI, newItem: FlowchartDisciplineUI): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FlowchartDisciplineUI, newItem: FlowchartDisciplineUI): Boolean {
            return oldItem == newItem
        }
    }
}
