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

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.Event
import com.forcetower.uefs.databinding.ItemEventListingBinding
import com.forcetower.uefs.feature.shared.inflate

class EventAdapter2 : ListAdapter<Event, EventAdapter2.EventHolder>(EventCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
        return EventHolder(parent.inflate(R.layout.item_event_listing))
    }

    override fun onBindViewHolder(holder: EventHolder, position: Int) {
        holder.binding.apply {
            event = getItem(position)
            executePendingBindings()
        }
    }

    class EventHolder(val binding: ItemEventListingBinding) : RecyclerView.ViewHolder(binding.root)
}

private object EventCallback : DiffUtil.ItemCallback<Event> () {
    override fun areItemsTheSame(oldItem: Event, newItem: Event) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem
}