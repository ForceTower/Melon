/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.feature.about

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.unes.core.model.Contributor
import com.forcetower.unes.databinding.ItemAboutContributorBinding
import com.forcetower.unes.feature.shared.inflater

class ContributorAdapter(
        private val listener: ContributorActions? = null
): ListAdapter<Contributor, ContributorHolder>(ContributorDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContributorHolder {
        val binding = ItemAboutContributorBinding.inflate(parent.inflater(), parent, false)
        return ContributorHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: ContributorHolder, position: Int) {
        holder.bind(getItem(position))
    }

}

class ContributorHolder(
    private val binding: ItemAboutContributorBinding,
    private val listener: ContributorActions? = null
): RecyclerView.ViewHolder(binding.root) {
    init {
        binding.root.setOnClickListener {_ -> onClick() }
    }

    private fun onClick() {
        val position = adapterPosition
        val item = binding.contributor
        if (item != null) listener?.onContributorClick(item, position)
    }

    fun bind(contributor: Contributor) {
        binding.contributor = contributor
        binding.executePendingBindings()
    }
}

object ContributorDiff: DiffUtil.ItemCallback<Contributor>() {
    override fun areItemsTheSame(oldItem: Contributor, newItem: Contributor): Boolean = oldItem.uid == newItem.uid
    override fun areContentsTheSame(oldItem: Contributor, newItem: Contributor): Boolean = oldItem == newItem
}

interface ContributorActions {
    fun onContributorClick(contributor: Contributor, position: Int)
}