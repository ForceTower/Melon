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

package com.forcetower.uefs.feature.reminders

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.Reminder
import com.forcetower.uefs.databinding.ItemReminderBinding
import com.forcetower.uefs.feature.shared.inflate
import com.forcetower.uefs.feature.shared.inflater

class RemindersAdapter(
    val lifecycleOwner: LifecycleOwner,
    val viewModel: RemindersViewModel
) : RecyclerView.Adapter<ReminderHolder>() {
    var currentReminders: List<Reminder> = emptyList()
        set(value) {
            field = value
            differ.submitList(buildMergedList(reminders = value))
        }

    private val differ = AsyncListDiffer(this, DiffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderHolder {
        return when (viewType) {
            R.layout.item_reminder -> ReminderHolder.ItemHolder(parent.inflate(viewType))
            R.layout.item_reminders_completed_header -> ReminderHolder.CompletedHeaderHolder(
                parent.inflater().inflate(viewType, parent, false)
            )
            else -> throw IllegalStateException("View type was not specified at position $viewType")
        }
    }

    override fun onBindViewHolder(holder: ReminderHolder, position: Int) {
        when (holder) {
            is ReminderHolder.ItemHolder -> {
                holder.binding.apply {
                    reminder = differ.currentList[position] as Reminder
                    listener = viewModel
                    lifecycleOwner = this@RemindersAdapter.lifecycleOwner
                    executePendingBindings()
                }
            }
            else -> Unit
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            Header -> R.layout.item_reminders_completed_header
            is Reminder -> R.layout.item_reminder
            else -> throw IllegalStateException("View type was not specified at position $position")
        }
    }

    private fun buildMergedList(reminders: List<Reminder>): List<Any> {
        val merged = mutableListOf<Any>()
        val incomplete = reminders.filter { !it.completed }
        val complete = reminders.filter { it.completed }

        merged.addAll(incomplete)
        if (complete.isNotEmpty()) {
            merged += Header
            merged.addAll(complete)
        }

        return merged
    }

    override fun getItemCount() = differ.currentList.size
}

sealed class ReminderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class ItemHolder(val binding: ItemReminderBinding) : ReminderHolder(binding.root)
    class CompletedHeaderHolder(itemView: View) : ReminderHolder(itemView)
}

private object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === Header && newItem === Header -> true
            oldItem is Reminder && newItem is Reminder -> oldItem.id == newItem.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is Reminder && newItem is Reminder -> oldItem == newItem
            else -> true
        }
    }
}

private object Header