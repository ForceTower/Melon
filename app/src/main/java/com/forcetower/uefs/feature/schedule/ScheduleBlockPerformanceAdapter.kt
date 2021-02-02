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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.core.utils.ColorUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.ui.ProcessedClassLocation
import com.forcetower.uefs.databinding.ItemScheduleBlockClassBinding
import com.forcetower.uefs.databinding.ItemScheduleBlockHeaderBinding
import com.forcetower.uefs.databinding.ItemScheduleBlockNothingBinding
import com.forcetower.uefs.databinding.ItemScheduleBlockTimeBinding
import com.forcetower.uefs.feature.shared.extensions.toWeekDay
import com.forcetower.uefs.feature.shared.inflate

class ScheduleBlockPerformanceAdapter(
    private val actions: ScheduleActions,
    private val showEmptyDays: Boolean,
    context: Context
) : RecyclerView.Adapter<ScheduleBlockPerformanceAdapter.ScheduleHolder>() {
    private val differ = AsyncListDiffer(this, ScheduleDiffCallback)
    var elements: Map<Int, List<ProcessedClassLocation>> = emptyMap()
        set(value) {
            field = value
            differ.submitList(buildList(value))
        }

    private val disciplineColors = mutableMapOf<String, Int>()
    private val colors = context.resources.getIntArray(R.array.discipline_colors)
    private var colorIndex = 0

    // Can be moved to a worker thread
    private fun buildList(values: Map<Int, List<ProcessedClassLocation>>): List<ProcessedClassLocation> {
        // the grid render is row by row... so...
        val result = mutableListOf<ProcessedClassLocation>()
        val referenceList = values[-1].orEmpty()

        val mutatedMap = values.toMutableMap().apply {
            if (showEmptyDays) {
                // Never on sunday or saturday, unless if already added
                (2..6).forEach {
                    if (!values.containsKey(it)) {
                        put(it, referenceList.map { ProcessedClassLocation.EmptySpace() })
                    }
                }
            }
        }.toMap()

        // The first row contains days
        result.add(ProcessedClassLocation.EmptySpace(true))
        result += mutatedMap.keys.sortedBy { it }.filter { it != -1 }.map {
            ProcessedClassLocation.DaySpace(it.toWeekDay(), it)
        }
        // the following rows contains time followed by locations
        val referenceMap = mutatedMap.entries.filter { it.key != -1 }.sortedBy { it.key }
        referenceList.forEachIndexed { index, element ->
            result.add(element)
            referenceMap.forEach {
                val location = it.value[index]
                result.add(location)
                if (location is ProcessedClassLocation.ElementSpace) {
                    val code = location.reference.groupData.classData.discipline.code
                    if (!disciplineColors.containsKey(code)) {
                        disciplineColors[code] = colorIndex++
                    }
                }
            }
        }
        return result
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleHolder {
        return when (viewType) {
            R.layout.item_schedule_block_nothing -> ScheduleHolder.NothingHolder(parent.inflate(viewType))
            R.layout.item_schedule_block_time -> ScheduleHolder.TimeHolder(parent.inflate(viewType))
            R.layout.item_schedule_block_header -> ScheduleHolder.HeaderHolder(parent.inflate(viewType))
            R.layout.item_schedule_block_class -> ScheduleHolder.ClassHolder(parent.inflate(viewType))
            else -> throw IllegalStateException("No view defined for view type $viewType")
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ScheduleHolder, position: Int) {
        when (holder) {
            is ScheduleHolder.HeaderHolder -> {
                val item = differ.currentList[position]
                holder.binding.day = (item as? ProcessedClassLocation.DaySpace)?.day ?: ""
            }
            is ScheduleHolder.TimeHolder -> {
                val item = differ.currentList[position]
                val casted = (item as ProcessedClassLocation.TimeSpace)
                holder.binding.timed = casted
            }
            is ScheduleHolder.ClassHolder -> {
                val item = differ.currentList[position]
                val casted = (item as ProcessedClassLocation.ElementSpace)
                holder.run {
                    val colorIndex = disciplineColors[casted.reference.groupData.classData.discipline.code] ?: 0
                    val color = colors[colorIndex]

                    binding.data = casted.reference.groupData
                    binding.scheduleActions = actions
                    binding.cardRoot.strokeColor = color
                    binding.cardRoot.setCardBackgroundColor(ColorUtils.modifyAlpha(color, 40))
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = differ.currentList[position]) {
            is ProcessedClassLocation.EmptySpace -> if (item.special) R.layout.item_schedule_block_header else R.layout.item_schedule_block_nothing
            is ProcessedClassLocation.TimeSpace -> R.layout.item_schedule_block_time
            is ProcessedClassLocation.DaySpace -> R.layout.item_schedule_block_header
            is ProcessedClassLocation.ElementSpace -> R.layout.item_schedule_block_class
            else -> throw IllegalStateException("No view type defined for $item")
        }
    }

    sealed class ScheduleHolder(view: View) : RecyclerView.ViewHolder(view) {
        class NothingHolder(val binding: ItemScheduleBlockNothingBinding) : ScheduleHolder(binding.root)
        class HeaderHolder(val binding: ItemScheduleBlockHeaderBinding) : ScheduleHolder(binding.root)
        class TimeHolder(val binding: ItemScheduleBlockTimeBinding) : ScheduleHolder(binding.root)
        class ClassHolder(val binding: ItemScheduleBlockClassBinding) : ScheduleHolder(binding.root)
    }
}
