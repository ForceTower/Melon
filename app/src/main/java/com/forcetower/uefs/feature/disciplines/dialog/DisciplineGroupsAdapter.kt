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

package com.forcetower.uefs.feature.disciplines.dialog

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.databinding.ItemSelectDisciplineGroupBinding
import com.forcetower.uefs.feature.common.DisciplineActions
import com.forcetower.uefs.feature.shared.inflate

class DisciplineGroupsAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val listener: DisciplineActions
) : ListAdapter<ClassGroup, GroupHolder>(GroupDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = GroupHolder(
        parent.inflate(R.layout.item_select_discipline_group)
    )

    override fun onBindViewHolder(holder: GroupHolder, position: Int) {
        holder.binding.apply {
            group = getItem(position)
            listener = this@DisciplineGroupsAdapter.listener
            lifecycleOwner = this@DisciplineGroupsAdapter.lifecycleOwner
            executePendingBindings()
        }
    }
}

class GroupHolder(val binding: ItemSelectDisciplineGroupBinding) : RecyclerView.ViewHolder(binding.root)

private object GroupDiff : DiffUtil.ItemCallback<ClassGroup>() {
    override fun areItemsTheSame(oldItem: ClassGroup, newItem: ClassGroup) = oldItem.uid == newItem.uid
    override fun areContentsTheSame(oldItem: ClassGroup, newItem: ClassGroup) = oldItem == newItem
}
