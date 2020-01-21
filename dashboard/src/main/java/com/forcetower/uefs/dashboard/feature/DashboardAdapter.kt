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

package com.forcetower.uefs.dashboard.feature

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.SStudent
import com.forcetower.uefs.core.storage.database.accessors.LocationWithGroup
import com.forcetower.uefs.dashboard.R
import com.forcetower.uefs.dashboard.databinding.ItemDashHeaderBinding
import com.forcetower.uefs.dashboard.databinding.ItemDashSagresMessageBinding
import com.forcetower.uefs.dashboard.databinding.ItemDashScheduleBinding
import com.forcetower.uefs.dashboard.databinding.ItemDashUpdatingAppBinding
import com.forcetower.uefs.feature.messages.disciplineText
import com.forcetower.uefs.feature.messages.messageContent
import com.forcetower.uefs.feature.messages.senderText
import com.forcetower.uefs.feature.shared.inflate

class DashboardAdapter(
    private val viewModel: DashboardViewModel,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<DashboardAdapter.DashboardHolder>() {
    var nextClass: LocationWithGroup? = null
    set(value) {
        field = value
        differ.submitList(buildMergedList(clazz = value))
    }

    var lastMessage: Message? = null
    set(value) {
        field = value
        differ.submitList(buildMergedList(message = value))
    }

    var student: SStudent? = null

    var updatingApp: Boolean = false
    set(value) {
        field = value
        differ.submitList(buildMergedList(updating = value))
    }

    private val differ = AsyncListDiffer(this, DiffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardHolder {
        return when (viewType) {
            R.layout.item_dash_header -> DashboardHolder.HeaderHolder(parent.inflate(viewType), viewModel, lifecycleOwner)
            R.layout.item_dash_schedule -> DashboardHolder.ScheduleHolder(parent.inflate(viewType))
            R.layout.item_dash_sagres_message -> DashboardHolder.MessageHolder(parent.inflate(viewType))
            R.layout.item_dash_updating_app -> DashboardHolder.UpdatingHolder(parent.inflate(viewType))
            else -> throw IllegalStateException("No view defined for viewType $viewType")
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun onBindViewHolder(holder: DashboardHolder, position: Int) {
        val item = differ.currentList[position]
        when (holder) {
            is DashboardHolder.ScheduleHolder -> {
                holder.binding.viewModel = viewModel
                holder.binding.lifecycleOwner = lifecycleOwner
                holder.binding.executePendingBindings()
            }
            is DashboardHolder.MessageHolder -> {
                val message = item as Message
                holder.binding.apply {
                    senderText(messageSender, message)
                    disciplineText(messageGroup, message)
                    messageContent(content, message.content)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            is Header -> R.layout.item_dash_header
            is Schedule -> R.layout.item_dash_schedule
            is Message -> R.layout.item_dash_sagres_message
            is UpdatingApp -> R.layout.item_dash_updating_app
            else -> throw IllegalStateException("No viewType defined for position $position")
        }
    }

    private fun buildMergedList(
        clazz: LocationWithGroup? = nextClass,
        message: Message? = lastMessage,
        updating: Boolean = updatingApp
    ): List<Any> {
        return mutableListOf<Any>(Header).apply {
            if (updating) {
                add(UpdatingApp)
            }

            add(Schedule(clazz))
//            if (clazz != null) {
//
//            }
            if (message != null) {
                add(message)
            }
        }
    }

    sealed class DashboardHolder(view: View) : RecyclerView.ViewHolder(view) {
        class HeaderHolder(
            binding: ItemDashHeaderBinding,
            viewModel: DashboardViewModel,
            lifecycleOwner: LifecycleOwner
        ) : DashboardHolder(binding.root) {
            init {
                binding.viewModel = viewModel
                binding.lifecycleOwner = lifecycleOwner
                binding.root.setTag(R.id.tag_header_id, "header")
            }
        }
        class ScheduleHolder(val binding: ItemDashScheduleBinding) : DashboardHolder(binding.root)
        class MessageHolder(val binding: ItemDashSagresMessageBinding) : DashboardHolder(binding.root)
        class UpdatingHolder(binding: ItemDashUpdatingAppBinding) : DashboardHolder(binding.root)
    }

    private object DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Header && newItem is Header -> true
                oldItem is UpdatingApp && newItem is UpdatingApp -> true
                oldItem is Schedule && newItem is Schedule -> true
                oldItem is Message && newItem is Message -> true
                else -> false
            }
        }
        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Schedule && newItem is Schedule -> true
                oldItem is Message && newItem is Message -> oldItem.content == newItem.content
                else -> true
            }
        }
    }

    private object Header
    private object UpdatingApp
    private data class Schedule(val clazz: LocationWithGroup?)
}