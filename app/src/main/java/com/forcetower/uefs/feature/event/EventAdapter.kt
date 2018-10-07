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

package com.forcetower.uefs.feature.event

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.Event
import com.forcetower.uefs.databinding.ItemEventCollapsedBinding
import com.forcetower.uefs.feature.shared.inflater

class EventAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: EventViewModel
): RecyclerView.Adapter<EventViewHolder>() {
    var currentEvents: List<Event> = emptyList()
    set(value) {
        field = value
        differ.submitList(buildMergedList(events = value))
    }

    private val differ = AsyncListDiffer<Any>(this, DiffCallback)

    init {
        differ.submitList(buildMergedList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val inflater = parent.inflater()
        return when (viewType) {
            R.layout.item_event_featured_header -> EventViewHolder.FeaturedHeaderHolder(
                inflater.inflate(viewType, parent, false)
            )
            R.layout.item_event_common_header -> EventViewHolder.CommonHeaderHolder(
                inflater.inflate(viewType, parent, false)
            )
            R.layout.item_event_collapsed -> EventViewHolder.EventHolder(
                ItemEventCollapsedBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalStateException("View was not defined for type $viewType")
        }
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        when (holder) {
            is EventViewHolder.FeaturedHeaderHolder -> Unit
            is EventViewHolder.CommonHeaderHolder -> Unit
            is EventViewHolder.EventHolder -> {
                holder.binding.event = differ.currentList[position] as Event
                holder.binding.setLifecycleOwner(lifecycleOwner)
                holder.binding.listener = viewModel
                holder.binding.executePendingBindings()
            }
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            FeatureHeader -> R.layout.item_event_featured_header
            CommonHeader -> R.layout.item_event_common_header
            is Event -> R.layout.item_event_collapsed
            else -> throw IllegalStateException("View type was not specified at position $position")
        }
    }

    private fun buildMergedList(
        events: List<Event> = currentEvents
    ): List<Any>? {
        val merged = mutableListOf<Any>()
        val featured = events.filter {  it.featured }
        val common   = events.filter { !it.featured }

        if (featured.isNotEmpty()) {
            merged += FeatureHeader
            merged.addAll(featured)
            merged += CommonHeader
        }

        merged.addAll(common)
        return merged
    }
}

sealed class EventViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    class FeaturedHeaderHolder(view: View): EventViewHolder(view)
    class CommonHeaderHolder(view: View): EventViewHolder(view)
    class EventHolder(val binding: ItemEventCollapsedBinding): EventViewHolder(binding.root)
}

private object DiffCallback: DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

private object FeatureHeader
private object CommonHeader