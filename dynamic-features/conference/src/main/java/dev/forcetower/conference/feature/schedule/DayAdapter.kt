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

package dev.forcetower.conference.feature.schedule

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.feature.shared.executeBindingsAfter
import com.forcetower.uefs.feature.shared.inflate
import dev.forcetower.conference.R
import dev.forcetower.conference.core.model.domain.DayIndicator
import dev.forcetower.conference.databinding.ItemScheduleDayIndicatorBinding

class DayAdapter(
    private val scheduleViewModel: ScheduleViewModel,
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<DayIndicator, DayAdapter.DayHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayHolder {
        return DayHolder(parent.inflate(R.layout.item_schedule_day_indicator))
    }

    override fun onBindViewHolder(holder: DayHolder, position: Int) {
        holder.binding.executeBindingsAfter {
            indicator = getItem(position)
            viewModel = scheduleViewModel
            lifecycleOwner = this@DayAdapter.lifecycleOwner
        }
    }

    inner class DayHolder(val binding: ItemScheduleDayIndicatorBinding) : RecyclerView.ViewHolder(binding.root)

    private object DiffCallback : DiffUtil.ItemCallback<DayIndicator>() {
        override fun areItemsTheSame(oldItem: DayIndicator, newItem: DayIndicator) = oldItem == newItem
        override fun areContentsTheSame(oldItem: DayIndicator, newItem: DayIndicator) =
            oldItem.areUiContentsTheSame(newItem)
    }
}
