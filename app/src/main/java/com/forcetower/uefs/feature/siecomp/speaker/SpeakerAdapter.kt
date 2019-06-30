/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.feature.siecomp.speaker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
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
