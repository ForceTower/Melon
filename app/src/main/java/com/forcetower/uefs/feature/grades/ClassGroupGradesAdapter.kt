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

package com.forcetower.uefs.feature.grades

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.databinding.ItemGradeOldBinding
import com.forcetower.uefs.feature.common.DisciplineActions
import com.forcetower.uefs.feature.shared.inflater

private const val GRADE = 8

class ClassGroupGradesAdapter(
    private val listener: DisciplineActions?
) : ListAdapter<Grade, GradesHolder>(GradesDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradesHolder {
        val binding = ItemGradeOldBinding.inflate(parent.inflater(), parent, false)
        return GradesHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: GradesHolder, position: Int) {
        holder.binding.apply {
            val item = getItem(position)
            grade = item
            executePendingBindings()
        }
    }

    override fun getItemViewType(position: Int) = GRADE
}

class GradesHolder(val binding: ItemGradeOldBinding, listener: DisciplineActions?) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.listener = listener
    }
}

private object GradesDiff : DiffUtil.ItemCallback<Grade>() {
    override fun areItemsTheSame(oldItem: Grade, newItem: Grade) = oldItem.uid == newItem.uid
    override fun areContentsTheSame(oldItem: Grade, newItem: Grade) = oldItem == newItem
}
