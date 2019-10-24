/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.feature.event

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.Event
import com.forcetower.uefs.databinding.ItemEventCollapsedBinding
import com.forcetower.uefs.feature.shared.inflater

class EventAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: EventViewModel
) : RecyclerView.Adapter<EventViewHolder>() {
    private var currentEvents: List<Event> = emptyList()
    set(value) {
        field = value
        differ.submitList(buildMergedList(events = value))
    }

    private val differ = AsyncListDiffer(this, DiffCallback)

    init {
        differ.submitList(buildMergedList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val inflater = parent.inflater()
        return when (viewType) {
            R.layout.item_event_featured_header -> EventViewHolder.FeaturedHeaderHolder(
                inflater.inflate(viewType, parent, false)
            )
            R.layout.item_event_common_header -> EventViewHolder.CommonHeaderHolder(
                inflater.inflate(viewType, parent, false)
            )
            R.layout.item_no_events -> EventViewHolder.NoEventHolder(
                inflater.inflate(viewType, parent, false)
            )
            R.layout.item_event_collapsed -> EventViewHolder.EventHolder(
                ItemEventCollapsedBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalStateException("View was not defined for type $viewType")
        }
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        when (holder) {
            is EventViewHolder.FeaturedHeaderHolder -> Unit
            is EventViewHolder.CommonHeaderHolder -> Unit
            is EventViewHolder.NoEventHolder -> Unit
            is EventViewHolder.EventHolder -> {
                holder.binding.event = differ.currentList[position] as Event
                holder.binding.lifecycleOwner = lifecycleOwner
                holder.binding.listener = viewModel
                holder.binding.executePendingBindings()
            }
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            FeatureHeader -> R.layout.item_event_featured_header
            CommonHeader -> R.layout.item_event_common_header
            NoEvents -> R.layout.item_no_events
            is Event -> R.layout.item_event_collapsed
            else -> throw IllegalStateException("View type was not specified at position $position")
        }
    }

    private fun buildMergedList(
        events: List<Event> = currentEvents
    ): List<Any>? {
        val merged = mutableListOf<Any>()
        if (events.isNotEmpty()) {
            val featured = events.filter { it.featured }
            val common = events.filter { !it.featured }

            if (featured.isNotEmpty()) {
                merged += FeatureHeader
                merged.addAll(featured)
            }

            if (common.isNotEmpty()) {
                merged += CommonHeader
                merged.addAll(common)
            }
        } else {
            merged += NoEvents
        }
        return merged
    }
}

sealed class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class FeaturedHeaderHolder(view: View) : EventViewHolder(view)
    class CommonHeaderHolder(view: View) : EventViewHolder(view)
    class NoEventHolder(view: View) : EventViewHolder(view)
    class EventHolder(val binding: ItemEventCollapsedBinding) : EventViewHolder(binding.root)
}

private object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === FeatureHeader && newItem === FeatureHeader -> true
            oldItem === CommonHeader && newItem === CommonHeader -> true
            oldItem === NoEvents && newItem === NoEvents -> true
            oldItem is Event && newItem is Event -> oldItem.id == newItem.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is Event && newItem is Event -> oldItem == newItem
            else -> true
        }
    }
}

private object FeatureHeader
private object CommonHeader
private object NoEvents