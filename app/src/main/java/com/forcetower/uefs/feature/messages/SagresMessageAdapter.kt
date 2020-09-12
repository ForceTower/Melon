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
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.databinding.ItemSagresMessageBinding
import com.forcetower.uefs.feature.shared.inflater

class SagresMessageAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: MessagesViewModel
) : PagedListAdapter<Message, MessageHolder>(MessagesDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val binding = ItemSagresMessageBinding.inflate(parent.inflater(), parent, false)
        return MessageHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        holder.binding.apply {
            message = getItem(position)
            listener = viewModel
            lifecycleOwner = this@SagresMessageAdapter.lifecycleOwner
            executePendingBindings()
        }
    }
}

class MessageHolder(val binding: ItemSagresMessageBinding) : RecyclerView.ViewHolder(binding.root)

object MessagesDiff : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.sagresId == newItem.sagresId
    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem == newItem
}
