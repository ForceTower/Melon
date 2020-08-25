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

package com.forcetower.uefs.feature.flowchart.home

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI
import com.forcetower.uefs.databinding.ItemFlowchartSemesterBinding
import com.forcetower.uefs.feature.shared.inflate

class SemesterAdapter(
    private val interactor: SemesterInteractor
) : ListAdapter<FlowchartSemesterUI, SemesterAdapter.SemesterViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SemesterViewHolder {
        return SemesterViewHolder(parent.inflate(R.layout.item_flowchart_semester), interactor)
    }

    override fun onBindViewHolder(holder: SemesterViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.semester = item
        holder.binding.executePendingBindings()
    }

    class SemesterViewHolder(val binding: ItemFlowchartSemesterBinding, interactor: SemesterInteractor) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.interactor = interactor
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FlowchartSemesterUI>() {
        override fun areItemsTheSame(oldItem: FlowchartSemesterUI, newItem: FlowchartSemesterUI): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FlowchartSemesterUI, newItem: FlowchartSemesterUI): Boolean {
            return oldItem == newItem
        }
    }
}
