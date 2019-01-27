/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
            setLifecycleOwner(lifecycleOwner)
            executePendingBindings()
        }
    }
}

class MessageHolder(val binding: ItemSagresMessageBinding) : RecyclerView.ViewHolder(binding.root)

object MessagesDiff : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.sagresId == newItem.sagresId
    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem == newItem
}