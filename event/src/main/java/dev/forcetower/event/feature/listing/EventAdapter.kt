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

package dev.forcetower.event.feature.listing

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.Event
import com.forcetower.uefs.feature.shared.executeBindingsAfter
import com.forcetower.uefs.feature.shared.inflate
import dev.forcetower.event.R
import dev.forcetower.event.databinding.ItemEventCollapsedBinding

class EventAdapter(
    private val actions: EventActions
) : ListAdapter<Event, EventAdapter.EventHolder>(EventDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
        return EventHolder(parent.inflate(R.layout.item_event_collapsed))
    }

    override fun onBindViewHolder(holder: EventHolder, position: Int) {
        val item = getItem(position)
        holder.binding.executeBindingsAfter {
            event = item
            actions = this@EventAdapter.actions
            root.setTag(R.id.tag_event_id, item.id)
        }
    }

    inner class EventHolder(val binding: ItemEventCollapsedBinding) : RecyclerView.ViewHolder(binding.root)

    private object EventDiff : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem
    }
}
