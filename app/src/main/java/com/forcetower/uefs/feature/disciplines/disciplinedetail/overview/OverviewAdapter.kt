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

package com.forcetower.uefs.feature.disciplines.disciplinedetail.overview

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.ClassLocation
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup
import com.forcetower.uefs.databinding.ItemDisciplineGoalsBinding
import com.forcetower.uefs.databinding.ItemDisciplineScheduleHideBinding
import com.forcetower.uefs.databinding.ItemDisciplineShortBinding
import com.forcetower.uefs.databinding.ItemDisciplineTeacherBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.shared.inflate
import com.forcetower.uefs.feature.shared.inflater

class OverviewAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: DisciplineViewModel
) : RecyclerView.Adapter<OverviewHolder>() {
    var currentClazz: ClassFullWithGroup? = null
        set(value) {
            field = value
            differ.submitList(buildMergedList(clazz = value))
        }

    var currentGroup: ClassGroup? = null
        set(value) {
            field = value
            differ.submitList(buildMergedList(group = value))
        }

    var currentSchedule = listOf<ClassLocation>()
        set(value) {
            field = value
            differ.submitList(buildMergedList(schedule = value))
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverviewHolder {
        val inflater = parent.inflater()
        return when (viewType) {
            R.layout.item_discipline_short -> OverviewHolder.ShortHolder(parent.inflate(viewType))
            R.layout.item_discipline_teacher -> OverviewHolder.TeacherHolder(parent.inflate(viewType))
            R.layout.item_discipline_goals -> OverviewHolder.ResumeHolder(parent.inflate(viewType))
            R.layout.item_discipline_schedule_hide -> OverviewHolder.ScheduleHolder(parent.inflate(viewType))
            else -> OverviewHolder.SimpleHolder(inflater.inflate(viewType, parent, false)) /* Draft or Statistics */
        }
    }

    override fun onBindViewHolder(holder: OverviewHolder, position: Int) {
        when (holder) {
            is OverviewHolder.SimpleHolder -> Unit
            is OverviewHolder.ShortHolder -> holder.binding.apply {
                lifecycleOwner = this@OverviewAdapter.lifecycleOwner
                viewModel = this@OverviewAdapter.viewModel
                executePendingBindings()
            }
            is OverviewHolder.TeacherHolder -> holder.binding.apply {
                lifecycleOwner = this@OverviewAdapter.lifecycleOwner
                viewModel = this@OverviewAdapter.viewModel
                executePendingBindings()
            }
            is OverviewHolder.ResumeHolder -> holder.binding.apply {
                lifecycleOwner = this@OverviewAdapter.lifecycleOwner
                viewModel = this@OverviewAdapter.viewModel
                executePendingBindings()
            }
            is OverviewHolder.ScheduleHolder -> holder.binding.apply {
                lifecycleOwner = this@OverviewAdapter.lifecycleOwner
                viewModel = this@OverviewAdapter.viewModel
                location = differ.currentList[position] as? ClassLocation
                executePendingBindings()
            }
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            is DisciplineDraft -> R.layout.item_discipline_draft_info
            is DisciplineShort -> R.layout.item_discipline_short
            is DisciplineTeacher -> R.layout.item_discipline_teacher
            is DisciplineResume -> R.layout.item_discipline_goals
            is Statistics -> R.layout.item_discipline_statistics_short
            is ScheduleHeader -> R.layout.item_discipline_schedule_header
            is ClassLocation -> R.layout.item_discipline_schedule_hide
            else -> throw IllegalStateException("No view type defined for position $position")
        }
    }

    private fun buildMergedList(
        clazz: ClassFullWithGroup? = currentClazz,
        group: ClassGroup? = currentGroup,
        schedule: List<ClassLocation> = currentSchedule
    ): List<Any> {
        val list = mutableListOf<Any>()
        if (clazz != null) {
            if (group?.draft == true) {
                list += DisciplineDraft
            }

            if (clazz.discipline.shortText != null) {
                list += DisciplineShort
            }

            if (group?.teacher != null) {
                list += DisciplineTeacher
            }

            if (clazz.discipline.resume != null) {
                list += DisciplineResume
            }

            if (schedule.isNotEmpty()) {
                list += ScheduleHeader
                list.addAll(schedule)
            }

            // list += Statistics
        }
        return list
    }

    private val differ = AsyncListDiffer<Any>(this, DiffCallback)
}

sealed class OverviewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class SimpleHolder(itemView: View) : OverviewHolder(itemView)
    class ShortHolder(val binding: ItemDisciplineShortBinding) : OverviewHolder(binding.root)
    class TeacherHolder(val binding: ItemDisciplineTeacherBinding) : OverviewHolder(binding.root)
    class ResumeHolder(val binding: ItemDisciplineGoalsBinding) : OverviewHolder(binding.root)
    class ScheduleHolder(val binding: ItemDisciplineScheduleHideBinding) : OverviewHolder(binding.root)
}

private object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === DisciplineDraft && newItem === DisciplineDraft -> true
            oldItem === DisciplineShort && newItem === DisciplineShort -> true
            oldItem === DisciplineTeacher && newItem === DisciplineTeacher -> true
            oldItem === DisciplineResume && newItem === DisciplineResume -> true
            oldItem === Statistics && newItem === Statistics -> true
            oldItem === ScheduleHeader && newItem === ScheduleHeader -> true
            oldItem is ClassLocation && newItem is ClassLocation -> oldItem.uid == newItem.uid
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is ClassLocation && newItem is ClassLocation -> oldItem == newItem
            else -> true
        }
    }
}

private object DisciplineDraft
private object DisciplineShort
private object DisciplineTeacher
private object DisciplineResume
private object Statistics
private object ScheduleHeader
