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

package com.forcetower.uefs.feature.messages

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.UMessage
import com.forcetower.uefs.databinding.ItemUnesMessageBinding
import com.forcetower.uefs.feature.shared.inflate

class UnesMessageAdapter(
    val lifecycleOwner: LifecycleOwner,
    val actions: MessagesActions
) : ListAdapter<UMessage, UMessageHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UMessageHolder(
        parent.inflate(R.layout.item_unes_message)
    )

    override fun onBindViewHolder(holder: UMessageHolder, position: Int) {
        holder.binding.apply {
            message = getItem(position)
            listener = actions
            lifecycleOwner = this@UnesMessageAdapter.lifecycleOwner
            executePendingBindings()
        }
    }
}

class UMessageHolder(val binding: ItemUnesMessageBinding) : RecyclerView.ViewHolder(binding.root)

private object DiffCallback : DiffUtil.ItemCallback<UMessage>() {
    override fun areItemsTheSame(oldItem: UMessage, newItem: UMessage) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: UMessage, newItem: UMessage) = oldItem == newItem
}
