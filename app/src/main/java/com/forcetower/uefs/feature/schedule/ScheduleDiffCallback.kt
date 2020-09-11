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

package com.forcetower.uefs.feature.schedule

import androidx.recyclerview.widget.DiffUtil
import com.forcetower.uefs.core.model.ui.ProcessedClassLocation

object ScheduleDiffCallback : DiffUtil.ItemCallback<ProcessedClassLocation>() {
    override fun areItemsTheSame(oldItem: ProcessedClassLocation, newItem: ProcessedClassLocation): Boolean {
        return when {
            oldItem is ProcessedClassLocation.TimeSpace && newItem is ProcessedClassLocation.TimeSpace -> oldItem.startInt == newItem.startInt
            oldItem is ProcessedClassLocation.ElementSpace && newItem is ProcessedClassLocation.ElementSpace -> oldItem.reference.location.uid == newItem.reference.location.uid
            oldItem is ProcessedClassLocation.EmptySpace && newItem is ProcessedClassLocation.EmptySpace -> true
            oldItem is ProcessedClassLocation.DaySpace && newItem is ProcessedClassLocation.DaySpace -> oldItem.dayInt == newItem.dayInt
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ProcessedClassLocation, newItem: ProcessedClassLocation): Boolean {
        return when {
            oldItem is ProcessedClassLocation.TimeSpace && newItem is ProcessedClassLocation.TimeSpace -> oldItem == newItem
            oldItem is ProcessedClassLocation.ElementSpace && newItem is ProcessedClassLocation.ElementSpace -> oldItem == newItem
            oldItem is ProcessedClassLocation.EmptySpace && newItem is ProcessedClassLocation.EmptySpace -> true
            oldItem is ProcessedClassLocation.DaySpace && newItem is ProcessedClassLocation.DaySpace -> oldItem == newItem
            else -> false
        }
    }
}
