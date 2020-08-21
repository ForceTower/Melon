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

package com.forcetower.uefs.easter.darktheme

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ItemDarkThemeUnlockBinding
import com.forcetower.uefs.feature.shared.inflate

class PreconditionsAdapter : ListAdapter<Precondition, PreconditionsAdapter.PreconditionHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreconditionHolder {
        return PreconditionHolder(parent.inflate(R.layout.item_dark_theme_unlock))
    }

    override fun onBindViewHolder(holder: PreconditionHolder, position: Int) {
        holder.binding.run {
            val item = getItem(position)
            precondition = item
            executePendingBindings()
        }
    }

    class PreconditionHolder(val binding: ItemDarkThemeUnlockBinding) : RecyclerView.ViewHolder(binding.root)

    private object DiffCallback : DiffUtil.ItemCallback<Precondition>() {
        override fun areItemsTheSame(oldItem: Precondition, newItem: Precondition) = oldItem.title == newItem.title
        override fun areContentsTheSame(oldItem: Precondition, newItem: Precondition) = oldItem == newItem
    }
}
