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

package com.forcetower.uefs.aeri.feature

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.aeri.R
import com.forcetower.uefs.aeri.core.model.Announcement
import com.forcetower.uefs.aeri.databinding.ItemAnnouncementBinding
import com.forcetower.uefs.feature.shared.inflate

class AERIMessagesAdapter(
    private val interactor: AnnouncementInteractor
) : PagedListAdapter<Announcement, AERIMessagesAdapter.AnnouncementHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementHolder {
        return AnnouncementHolder(parent.inflate(R.layout.item_announcement), interactor)
    }

    override fun onBindViewHolder(holder: AnnouncementHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            announcement = item
        }
    }

    inner class AnnouncementHolder(val binding: ItemAnnouncementBinding, interactor: AnnouncementInteractor) : RecyclerView.ViewHolder(binding.root) {
        init { binding.interactor = interactor }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Announcement>() {
        override fun areItemsTheSame(oldItem: Announcement, newItem: Announcement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Announcement, newItem: Announcement): Boolean {
            return oldItem == newItem
        }
    }
}
