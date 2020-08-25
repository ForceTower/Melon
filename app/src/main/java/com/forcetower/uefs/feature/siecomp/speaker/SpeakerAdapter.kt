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

package com.forcetower.uefs.feature.siecomp.speaker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.core.adapters.ImageLoadListener
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ItemEventSpeakerInfoBinding
import timber.log.Timber

class SpeakerAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val speakerViewModel: SIECOMPSpeakerViewModel,
    private val headLoadListener: ImageLoadListener
) : RecyclerView.Adapter<SpeakerViewHolder>() {

    private val differ = AsyncListDiffer(this, DiffCallback)

    init {
        differ.submitList(buildMergedList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeakerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_event_speaker_info -> SpeakerViewHolder.SpeakerInfoViewHolder(
                ItemEventSpeakerInfoBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: SpeakerViewHolder, position: Int) {
        when (holder) {
            is SpeakerViewHolder.SpeakerInfoViewHolder -> holder.binding.apply {
                viewModel = speakerViewModel
                headshotImageListener = headLoadListener
                lifecycleOwner = this@SpeakerAdapter.lifecycleOwner
                executePendingBindings()
                Timber.d("Executing")
            }
        }
    }

    override fun getItemCount() = differ.currentList.size.also { Timber.d("Size is: $it") }

    private fun buildMergedList(): List<Any> {
        return mutableListOf<Any>(SpeakerItem)
    }

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            SpeakerItem -> R.layout.item_event_speaker_info
            else -> throw IllegalStateException("Unknown view type at position $position")
        }
    }
}

object SpeakerItem

object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === SpeakerItem && newItem === SpeakerItem -> true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return true
    }
}

sealed class SpeakerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class SpeakerInfoViewHolder(
        val binding: ItemEventSpeakerInfoBinding
    ) : SpeakerViewHolder(binding.root)
}
