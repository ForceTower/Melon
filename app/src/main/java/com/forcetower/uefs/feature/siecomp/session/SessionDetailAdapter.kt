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

package com.forcetower.uefs.feature.siecomp.session

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.siecomp.Session
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.databinding.ItemEventSessionInfoBinding
import com.forcetower.uefs.databinding.ItemEventSpeakerBinding
import com.forcetower.uefs.feature.shared.inflater

class SessionDetailAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val sessionViewModel: SIECOMPSessionViewModel
) : RecyclerView.Adapter<SessionDetailViewHolder>() {
    var speakers: List<Speaker> = emptyList()
        set(value) {
            field = value
            differ.submitList(buildMergedList(sessionSpeakers = value))
        }

    private val differ = AsyncListDiffer<Any>(this, DiffCallback)

    init {
        differ.submitList(buildMergedList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionDetailViewHolder {
        val inflater = parent.inflater()

        return when (viewType) {
            R.layout.item_event_session_info -> SessionDetailViewHolder.SessionInfoViewHolder(
                ItemEventSessionInfoBinding.inflate(inflater, parent, false)
            )
            R.layout.item_event_speaker_header -> SessionDetailViewHolder.HeaderViewHolder(
                inflater.inflate(viewType, parent, false)
            )
            R.layout.item_event_speaker -> SessionDetailViewHolder.SpeakerViewHolder(
                ItemEventSpeakerBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: SessionDetailViewHolder, position: Int) {
        when (holder) {
            is SessionDetailViewHolder.SessionInfoViewHolder -> holder.binding.apply {
                viewModel = sessionViewModel
                lifecycleOwner = this@SessionDetailAdapter.lifecycleOwner
                executePendingBindings()
            }
            is SessionDetailViewHolder.SpeakerViewHolder -> holder.binding.apply {
                val presenter = differ.currentList[position] as Speaker
                speaker = presenter
                listener = sessionViewModel
                lifecycleOwner = this@SessionDetailAdapter.lifecycleOwner
                root.setTag(R.id.tag_speaker_id, presenter.uid)
                executePendingBindings()
            }
            is SessionDetailViewHolder.HeaderViewHolder -> Unit // no-op
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            is SessionItem -> R.layout.item_event_session_info
            is SpeakerHeaderItem -> R.layout.item_event_speaker_header
            is Speaker -> R.layout.item_event_speaker
            else -> throw IllegalStateException("Unknown view type at position $position")
        }
    }

    private fun buildMergedList(
        sessionSpeakers: List<Speaker> = speakers
    ): List<Any> {
        val merged = mutableListOf<Any>(SessionItem)
        if (sessionSpeakers.isNotEmpty()) {
            merged += SpeakerHeaderItem
            merged.addAll(sessionSpeakers)
        }
        return merged
    }
}

object SessionItem
object SpeakerHeaderItem

/**
 * Diff items presented by this adapter.
 */
object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === SessionItem && newItem === SessionItem -> true
            oldItem === SpeakerHeaderItem && newItem === SpeakerHeaderItem -> true
            oldItem is Speaker && newItem is Speaker -> oldItem.uid == newItem.uid
            oldItem is Session && newItem is Session -> oldItem.uid == newItem.uid
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is Speaker && newItem is Speaker -> oldItem == newItem
            oldItem is Session && newItem is Session -> oldItem == newItem
            else -> true
        }
    }
}

/**
 * [RecyclerView.ViewHolder] types used by this adapter.
 */
sealed class SessionDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class SessionInfoViewHolder(
        val binding: ItemEventSessionInfoBinding
    ) : SessionDetailViewHolder(binding.root)

    class SpeakerViewHolder(
        val binding: ItemEventSpeakerBinding
    ) : SessionDetailViewHolder(binding.root)

    class HeaderViewHolder(
        itemView: View
    ) : SessionDetailViewHolder(itemView)
}
