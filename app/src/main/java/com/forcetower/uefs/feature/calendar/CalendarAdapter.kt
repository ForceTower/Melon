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

package com.forcetower.uefs.feature.calendar

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.CalendarItem
import com.forcetower.uefs.databinding.ItemAcademicEventBinding
import com.forcetower.uefs.feature.shared.inflater

class CalendarAdapter : ListAdapter<CalendarItem, CalendarHolder>(DiffCalendar) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarHolder {
        return CalendarHolder(ItemAcademicEventBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: CalendarHolder, position: Int) {
        holder.binding.event = getItem(position)
        holder.binding.executePendingBindings()
    }
}

class CalendarHolder(val binding: ItemAcademicEventBinding) : RecyclerView.ViewHolder(binding.root)

private object DiffCalendar : DiffUtil.ItemCallback<CalendarItem>() {
    override fun areItemsTheSame(oldItem: CalendarItem, newItem: CalendarItem) = oldItem.uid == newItem.uid
    override fun areContentsTheSame(oldItem: CalendarItem, newItem: CalendarItem) = oldItem == newItem
}
