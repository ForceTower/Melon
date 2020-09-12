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

package com.forcetower.uefs.feature.disciplines.disciplinedetail.classes

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.databinding.ItemDisciplineClassItemBinding
import com.forcetower.uefs.feature.shared.inflate

class ClassesAdapter(
    private val actions: ClassesActions
) : ListAdapter<ClassItem, ClassesAdapter.ClassHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ClassHolder(parent.inflate(R.layout.item_discipline_class_item), actions)

    override fun onBindViewHolder(holder: ClassHolder, position: Int) {
        holder.binding.apply {
            classItem = getItem(position)
            executePendingBindings()
        }
    }

    inner class ClassHolder(
        val binding: ItemDisciplineClassItemBinding,
        actions: ClassesActions
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.actions = actions
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ClassItem>() {
        override fun areItemsTheSame(oldItem: ClassItem, newItem: ClassItem) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: ClassItem, newItem: ClassItem) = oldItem == newItem
    }
}
