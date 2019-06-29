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
            lifecycleOwner = lifecycleOwner
            executePendingBindings()
        }
    }
}

class UMessageHolder(val binding: ItemUnesMessageBinding) : RecyclerView.ViewHolder(binding.root)

private object DiffCallback : DiffUtil.ItemCallback<UMessage>() {
    override fun areItemsTheSame(oldItem: UMessage, newItem: UMessage) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: UMessage, newItem: UMessage) = oldItem == newItem
}