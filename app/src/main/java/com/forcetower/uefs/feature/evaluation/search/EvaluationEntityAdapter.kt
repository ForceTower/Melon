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

package com.forcetower.uefs.feature.evaluation.search

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.EdgeParadoxSearchableItem
import com.forcetower.uefs.databinding.ItemEvaluationSimpleEntityBinding
import com.forcetower.uefs.feature.shared.inflate

class EvaluationEntityAdapter(
    private val selector: EntitySelector
) : PagingDataAdapter<EdgeParadoxSearchableItem, EvaluationEntityAdapter.EntityHolder>(EntityDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = EntityHolder(
        parent.inflate(R.layout.item_evaluation_simple_entity),
        selector
    )

    override fun onBindViewHolder(holder: EntityHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            entity = item
            root.setTag(R.id.tag_student_id, item?.serviceId)
            executePendingBindings()
        }
    }

    inner class EntityHolder(
        val binding: ItemEvaluationSimpleEntityBinding,
        selector: EntitySelector
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.interactor = selector
        }
    }

    private object EntityDiff : DiffUtil.ItemCallback<EdgeParadoxSearchableItem>() {
        override fun areItemsTheSame(oldItem: EdgeParadoxSearchableItem, newItem: EdgeParadoxSearchableItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EdgeParadoxSearchableItem, newItem: EdgeParadoxSearchableItem) = oldItem == newItem
    }
}
