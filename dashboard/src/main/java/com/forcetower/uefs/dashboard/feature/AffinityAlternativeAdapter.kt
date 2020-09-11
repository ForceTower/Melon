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

package com.forcetower.uefs.dashboard.feature

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.SStudent
import com.forcetower.uefs.core.storage.database.aggregation.AffinityQuestionFull
import com.forcetower.uefs.dashboard.R
import com.forcetower.uefs.dashboard.databinding.ItemDashAffinityPersonChipBinding
import com.forcetower.uefs.feature.shared.inflate

class AffinityAlternativeAdapter(
    private val listener: AffinityListener?,
    private val question: AffinityQuestionFull?
) : ListAdapter<SStudent, AffinityAlternativeAdapter.StudentHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentHolder {
        return StudentHolder(parent.inflate(R.layout.item_dash_affinity_person_chip), listener, question)
    }

    override fun onBindViewHolder(holder: StudentHolder, position: Int) {
        holder.binding.student = getItem(position)
    }

    inner class StudentHolder(
        val binding: ItemDashAffinityPersonChipBinding,
        listener: AffinityListener?,
        question: AffinityQuestionFull?
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.listener = listener
            binding.affinity = question
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SStudent>() {
        override fun areItemsTheSame(oldItem: SStudent, newItem: SStudent) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SStudent, newItem: SStudent) = oldItem == newItem
    }
}
