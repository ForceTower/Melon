/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.feature.messages

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.unes.core.model.Message
import com.forcetower.unes.databinding.ItemSagresMessageBinding
import com.forcetower.unes.feature.shared.inflater

class SagresMessageAdapter(diffCallback: DiffUtil.ItemCallback<Message>):
        PagedListAdapter<Message, SagresMessageAdapter.MessageHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val binding = ItemSagresMessageBinding.inflate(parent.inflater(), parent, false)
        return MessageHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageHolder(val binding: ItemSagresMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message?) {
            binding.message = message
        }
    }
}