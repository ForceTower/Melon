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

package com.forcetower.uefs.feature.mechcalculator

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ItemMechValueBinding
import com.forcetower.uefs.feature.shared.inflate

class MechanicsAdapter(
    val lifecycleOwner: LifecycleOwner,
    val viewModel: MechanicalViewModel
) : ListAdapter<MechValue, MechanicsAdapter.MechHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MechHolder(
        parent.inflate(R.layout.item_mech_value)
    )

    override fun onBindViewHolder(holder: MechHolder, position: Int) {
        holder.binding.run {
            item = getItem(position)
            interactor = viewModel
            lifecycleOwner = this@MechanicsAdapter.lifecycleOwner
            executePendingBindings()
        }
    }

    inner class MechHolder(val binding: ItemMechValueBinding) : RecyclerView.ViewHolder(binding.root)
    private object DiffCallback : DiffUtil.ItemCallback<MechValue>() {
        override fun areItemsTheSame(oldItem: MechValue, newItem: MechValue) = oldItem.uuid == newItem.uuid
        override fun areContentsTheSame(oldItem: MechValue, newItem: MechValue) = oldItem == newItem
    }
}
