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

package com.forcetower.uefs.feature.adventure

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.AchDistance
import com.forcetower.uefs.databinding.ItemAdventureDistanceBinding
import com.forcetower.uefs.feature.shared.inflate

class DistanceAdapter : ListAdapter<AchDistance, DistanceAdapter.DistanceHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DistanceHolder(
        parent.inflate(R.layout.item_adventure_distance)
    )

    override fun onBindViewHolder(holder: DistanceHolder, position: Int) {
        holder.binding.run {
            distance = getItem(position)
            executePendingBindings()
        }
    }

    class DistanceHolder(val binding: ItemAdventureDistanceBinding) : RecyclerView.ViewHolder(binding.root)

    private object DiffCallback : DiffUtil.ItemCallback<AchDistance>() {
        override fun areItemsTheSame(oldItem: AchDistance, newItem: AchDistance) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: AchDistance, newItem: AchDistance) = oldItem.distance == newItem.distance
    }
}
