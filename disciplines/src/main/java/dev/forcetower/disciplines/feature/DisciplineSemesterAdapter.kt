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

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.ui.disciplines.CheckableSemester
import com.forcetower.uefs.feature.shared.inflate
import dev.forcetower.disciplines.R
import dev.forcetower.disciplines.databinding.ItemDisciplinesSemesterIndicatorBinding

class DisciplineSemesterAdapter(
    private val actions: DisciplinesSemestersActions
) : ListAdapter<CheckableSemester, DisciplineSemesterAdapter.SemesterHolder>(SemesterDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SemesterHolder {
        return SemesterHolder(parent.inflate(R.layout.item_disciplines_semester_indicator))
    }

    override fun onBindViewHolder(holder: SemesterHolder, position: Int) {
        holder.binding.apply {
            element = getItem(position)
            executePendingBindings()
        }
    }

    inner class SemesterHolder(val binding: ItemDisciplinesSemesterIndicatorBinding) : RecyclerView.ViewHolder(binding.root) {
        init { binding.actions = actions }
    }

    private object SemesterDiff : DiffUtil.ItemCallback<CheckableSemester>() {
        override fun areItemsTheSame(oldItem: CheckableSemester, newItem: CheckableSemester) = oldItem == newItem
        override fun areContentsTheSame(oldItem: CheckableSemester, newItem: CheckableSemester) = oldItem.areUiContentsTheSame(newItem)
    }
}
