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

package com.forcetower.uefs.feature.syncregistry

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.SyncRegistry
import com.forcetower.uefs.databinding.ItemSyncRegistryBinding
import com.forcetower.uefs.feature.shared.inflater

class SyncRegistryAdapter : PagedListAdapter<SyncRegistry, SyncRegistryHolder>(SyncDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyncRegistryHolder {
        return SyncRegistryHolder(
            ItemSyncRegistryBinding.inflate(parent.inflater(), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SyncRegistryHolder, position: Int) {
        holder.binding.registry = getItem(position)
    }
}

class SyncRegistryHolder(
    val binding: ItemSyncRegistryBinding
) : RecyclerView.ViewHolder(binding.root)

object SyncDiff : DiffUtil.ItemCallback<SyncRegistry>() {
    override fun areItemsTheSame(oldItem: SyncRegistry, newItem: SyncRegistry): Boolean {
        return oldItem.uid == newItem.uid
    }

    override fun areContentsTheSame(oldItem: SyncRegistry, newItem: SyncRegistry): Boolean {
        return oldItem == newItem
    }
}
