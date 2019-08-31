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

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.storage.database.accessors.ClassWithGroups
import com.forcetower.uefs.databinding.ItemDisciplineCollapsedBinding
import com.forcetower.uefs.feature.shared.inflater

class DisciplineSemesterAdapter(
    private val viewModel: DisciplineViewModel
) : ListAdapter<ClassWithGroups, ClassHolder>(ClassDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassHolder {
        val binding = ItemDisciplineCollapsedBinding.inflate(parent.inflater(), parent, false)
        return ClassHolder(binding)
    }

    override fun onBindViewHolder(holder: ClassHolder, position: Int) {
        holder.binding.apply {
            clazzGroup = getItem(position)
            listener = viewModel
            executePendingBindings()
        }
    }

    override fun getItemViewType(position: Int) = DISCIPLINE

    companion object {
        private const val DISCIPLINE = 4
    }
}

class ClassHolder(val binding: ItemDisciplineCollapsedBinding) : RecyclerView.ViewHolder(binding.root)

private object ClassDiff : DiffUtil.ItemCallback<ClassWithGroups>() {
    override fun areItemsTheSame(oldItem: ClassWithGroups, newItem: ClassWithGroups) = oldItem.clazz.uid == newItem.clazz.uid && oldItem.discipline().uid == newItem.discipline().uid
    override fun areContentsTheSame(oldItem: ClassWithGroups, newItem: ClassWithGroups) = oldItem == newItem
}