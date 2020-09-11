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

package com.forcetower.uefs.feature.about

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.Contributor
import com.forcetower.uefs.databinding.ItemAboutContributorBinding
import com.forcetower.uefs.feature.shared.inflater

class ContributorAdapter(
    private val listener: ContributorActions? = null
) : ListAdapter<Contributor, ContributorHolder>(ContributorDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContributorHolder {
        val binding = ItemAboutContributorBinding.inflate(parent.inflater(), parent, false)
        return ContributorHolder(binding)
    }

    override fun onBindViewHolder(holder: ContributorHolder, position: Int) {
        holder.binding.apply {
            actions = listener
            contributor = getItem(position)
            executePendingBindings()
        }
    }
}

class ContributorHolder(val binding: ItemAboutContributorBinding) : RecyclerView.ViewHolder(binding.root)

private object ContributorDiff : DiffUtil.ItemCallback<Contributor>() {
    override fun areItemsTheSame(oldItem: Contributor, newItem: Contributor) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Contributor, newItem: Contributor) = oldItem == newItem
}

interface ContributorActions {
    fun onContributorClick(contributor: Contributor?)
}
