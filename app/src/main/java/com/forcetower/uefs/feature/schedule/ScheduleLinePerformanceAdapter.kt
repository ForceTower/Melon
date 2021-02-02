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

package com.forcetower.uefs.feature.schedule

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.ui.ProcessedClassLocation
import com.forcetower.uefs.databinding.ItemScheduleLineClassBinding
import com.forcetower.uefs.databinding.ItemScheduleLineDayPerformanceBinding
import com.forcetower.uefs.feature.shared.inflate

class ScheduleLinePerformanceAdapter(
    private val actions: ScheduleActions
) : RecyclerView.Adapter<ScheduleLinePerformanceAdapter.ScheduleHolder>() {
    private val differ = AsyncListDiffer(this, ScheduleDiffCallback)
    var elements: List<ProcessedClassLocation> = emptyList()
        set(value) {
            field = value
            differ.submitList(value)
        }

    sealed class ScheduleHolder(view: View) : RecyclerView.ViewHolder(view) {
        class DayHolder(val binding: ItemScheduleLineDayPerformanceBinding) : ScheduleHolder(binding.root)
        class ClassHolder(val binding: ItemScheduleLineClassBinding) : ScheduleHolder(binding.root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleHolder {
        return when (viewType) {
            R.layout.item_schedule_line_day_performance -> ScheduleHolder.DayHolder(parent.inflate(viewType))
            R.layout.item_schedule_line_class -> ScheduleHolder.ClassHolder(parent.inflate(viewType))
            else -> throw IllegalStateException("No view defined for view type $viewType")
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ScheduleHolder, position: Int) {
        when (holder) {
            is ScheduleHolder.DayHolder -> {
                val item = differ.currentList[position] as ProcessedClassLocation.DaySpace
                holder.run {
                    binding.textScheduleDay.text = item.day
                }
            }
            is ScheduleHolder.ClassHolder -> {
                val item = differ.currentList[position] as ProcessedClassLocation.ElementSpace
                holder.run {
                    binding.actions = actions
                    binding.data = item.reference
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = differ.currentList[position]) {
            is ProcessedClassLocation.DaySpace -> R.layout.item_schedule_line_day_performance
            is ProcessedClassLocation.ElementSpace -> R.layout.item_schedule_line_class
            else -> throw IllegalStateException("No viewType defined for $item")
        }
    }
}
