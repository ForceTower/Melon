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

package com.forcetower.uefs.feature.siecomp.schedule

import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.siecomp.Tag
import com.forcetower.uefs.core.storage.eventdatabase.accessors.SessionWithData
import com.forcetower.uefs.databinding.ItemEventSessionBinding
import com.forcetower.uefs.databinding.ItemEventSessionTagBinding
import com.forcetower.uefs.feature.shared.inflater
import com.forcetower.uefs.feature.siecomp.SIECOMPEventViewModel
import com.forcetower.uefs.widget.UnscrollableFlexboxLayoutManager
import java.time.ZoneId

class ScheduleDayAdapter(
    private val viewModel: SIECOMPEventViewModel,
    private val tagViewPool: RecyclerView.RecycledViewPool,
    private val zone: ZoneId
) : ListAdapter<SessionWithData, SessionHolder>(SessionDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionHolder {
        val binding = ItemEventSessionBinding.inflate(parent.inflater(), parent, false).apply {
            listener = viewModel
        }
        return SessionHolder(binding, tagViewPool, zone)
    }

    override fun onBindViewHolder(holder: SessionHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TagsAdapter : ListAdapter<Tag, TagHolder>(TagDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagHolder {
        val binding = ItemEventSessionTagBinding.inflate(parent.inflater(), parent, false)
        return TagHolder(binding)
    }

    override fun onBindViewHolder(holder: TagHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class SessionHolder(
    private val binding: ItemEventSessionBinding,
    private val tagViewPool: RecyclerView.RecycledViewPool,
    private val zone: ZoneId
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.recyclerTags.apply {
            setRecycledViewPool(tagViewPool)
            layoutManager = UnscrollableFlexboxLayoutManager(binding.root.context).apply {
                recycleChildrenOnDetach = true
            }
            itemAnimator = DefaultItemAnimator()
        }
    }

    fun bind(session: SessionWithData) {
        binding.data = session
        binding.zone = zone
        binding.executePendingBindings()
    }
}

class TagHolder(
    private val binding: ItemEventSessionTagBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(tag: Tag) {
        binding.tag = tag
        binding.executePendingBindings()
    }
}

object SessionDiff : DiffUtil.ItemCallback<SessionWithData>() {
    override fun areItemsTheSame(oldItem: SessionWithData, newItem: SessionWithData): Boolean {
        return oldItem.session.uid == newItem.session.uid
    }

    override fun areContentsTheSame(oldItem: SessionWithData, newItem: SessionWithData): Boolean {
        return oldItem == newItem
    }
}

object TagDiff : DiffUtil.ItemCallback<Tag>() {
    override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
        return oldItem.uid == newItem.uid
    }

    override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean {
        return oldItem == newItem
    }
}
