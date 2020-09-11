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
