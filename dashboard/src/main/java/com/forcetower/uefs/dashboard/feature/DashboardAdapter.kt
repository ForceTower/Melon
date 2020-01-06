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
import com.forcetower.uefs.core.storage.database.accessors.LocationWithGroup
import com.forcetower.uefs.dashboard.R
import com.forcetower.uefs.dashboard.databinding.ItemDashHeaderBinding
import com.forcetower.uefs.dashboard.databinding.ItemDashScheduleBinding
import com.forcetower.uefs.feature.shared.inflate

class DashboardAdapter(
    private val viewModel: DashboardViewModel,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<DashboardAdapter.DashboardHolder>() {
    var element1: Any? = null
    set(value) {
        field = value
        differ.submitList(buildMergedList())
    }

    var nextClass: LocationWithGroup? = null
    set(value) {
        field = value
        differ.submitList(buildMergedList(nextClass = value))
    }

    private val differ = AsyncListDiffer(this, DiffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardHolder {
        return when (viewType) {
            R.layout.item_dash_header -> DashboardHolder.HeaderHolder(parent.inflate(viewType), viewModel, lifecycleOwner)
            R.layout.item_dash_schedule -> DashboardHolder.ScheduleHolder(parent.inflate(viewType))
            else -> throw IllegalStateException("No view defined for viewType $viewType")
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun onBindViewHolder(holder: DashboardHolder, position: Int) {
        val item = differ.currentList[position]
        when (holder) {
            is DashboardHolder.ScheduleHolder -> {
                val location = (item as Schedule).clazz
                holder.binding.location = location
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            is Header -> R.layout.item_dash_header
            is Schedule -> R.layout.item_dash_schedule
            else -> throw IllegalStateException("No viewType defined for position $position")
        }
    }

    private fun buildMergedList(
        nextClass: LocationWithGroup? = null
    ): List<Any> {
        return mutableListOf<Any>(Header).apply {
            if (nextClass != null) {
                add(Schedule(nextClass))
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
            }
        }
        class ScheduleHolder(val binding: ItemDashScheduleBinding) : DashboardHolder(binding.root)
    }

    private object DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any) = false
        override fun areContentsTheSame(oldItem: Any, newItem: Any) = false
    }

    private object Header
    private data class Schedule(val clazz: LocationWithGroup)
}