/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.unes.feature.siecomp.day

import android.graphics.Color
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.unes.core.model.event.Tag
import com.forcetower.unes.core.storage.database.accessors.SessionWithData
import com.forcetower.unes.databinding.ItemEventSessionBinding
import com.forcetower.unes.databinding.ItemEventSessionTagBinding
import com.forcetower.unes.feature.shared.inflater
import com.forcetower.unes.widget.UnscrollableFlexboxLayoutManager
import org.threeten.bp.ZoneId
import timber.log.Timber

class EScheduleDayAdapter(
    private val tagViewPool: RecyclerView.RecycledViewPool,
    private val zone: ZoneId
): ListAdapter<SessionWithData, SessionHolder>(SessionDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionHolder {
        val binding = ItemEventSessionBinding.inflate(parent.inflater(), parent, false)
        return SessionHolder(binding, tagViewPool, zone)
    }

    override fun onBindViewHolder(holder: SessionHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TagsAdapter: ListAdapter<Tag, TagHolder>(TagDiff) {
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
): RecyclerView.ViewHolder(binding.root) {
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
): RecyclerView.ViewHolder(binding.root) {
    fun bind(tag: Tag) {
        binding.tag = tag
        binding.executePendingBindings()
    }
}

object SessionDiff: DiffUtil.ItemCallback<SessionWithData>() {
    override fun areItemsTheSame(oldItem: SessionWithData, newItem: SessionWithData): Boolean {
        return oldItem.session.uid == newItem.session.uid
    }

    override fun areContentsTheSame(oldItem: SessionWithData, newItem: SessionWithData): Boolean {
        return oldItem == newItem
    }
}

object TagDiff: DiffUtil.ItemCallback<Tag>() {
    override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
        return oldItem.uid == newItem.uid
    }

    override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean {
        return oldItem == newItem
    }

}