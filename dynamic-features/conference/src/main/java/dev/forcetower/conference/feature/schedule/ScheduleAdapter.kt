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
import dev.forcetower.conference.core.model.persistence.Session
import dev.forcetower.conference.databinding.ItemScheduleSessionBinding

class ScheduleAdapter(
    private val viewModel: ScheduleViewModel,
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<Session, ScheduleAdapter.SessionHolder>(SessionDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionHolder {
        return SessionHolder(parent.inflate(R.layout.item_schedule_session))
    }

    override fun onBindViewHolder(holder: SessionHolder, position: Int) {
        holder.binding.executeBindingsAfter {
            sessionData = getItem(position)
            actions = viewModel
            lifecycleOwner = this@ScheduleAdapter.lifecycleOwner
        }
    }

    inner class SessionHolder(val binding: ItemScheduleSessionBinding) : RecyclerView.ViewHolder(binding.root)

    private object SessionDiff : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Session, newItem: Session) = oldItem == newItem
    }
}
