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

package com.forcetower.uefs.feature.demand

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.sagres.database.model.SagresDemandOffer
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ItemCardDemandOfferRevBinding
import com.forcetower.uefs.databinding.ItemDemandHeaderBinding
import com.forcetower.uefs.feature.shared.inflate
import timber.log.Timber

class DemandOffersAdapter(
    val lifecycleOwner: LifecycleOwner,
    val viewModel: DemandViewModel
) : RecyclerView.Adapter<DemandHolder>() {
    private val differ = AsyncListDiffer(this, DiffCallback)

    var currentList: List<SagresDemandOffer> = listOf()
        set(value) {
            field = value
            differ.submitList(buildMergedList(offers = currentList))
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemandHolder {
        return when (viewType) {
            R.layout.item_card_demand_offer_rev -> DemandHolder.OfferHolder(parent.inflate(viewType))
            R.layout.item_demand_header -> DemandHolder.HeaderHolder(parent.inflate(viewType))
            else -> throw IllegalStateException("Unable to inflate $viewType")
        }
    }

    override fun onBindViewHolder(holder: DemandHolder, position: Int) {
        when (holder) {
            is DemandHolder.OfferHolder -> holder.binding.apply {
                offer = differ.currentList[position] as SagresDemandOffer
                lifecycleOwner = this@DemandOffersAdapter.lifecycleOwner
                actions = viewModel
                executePendingBindings()
            }
            is DemandHolder.HeaderHolder -> {
                val ctx = holder.itemView.context
                val string = when (differ.currentList[position]) {
                    is AvailableHeader -> ctx.getString(R.string.demand_status_available)
                    is CurrentHeader -> ctx.getString(R.string.demand_status_current)
                    is CompletedHeader -> ctx.getString(R.string.demand_status_completed)
                    is LockedHeader -> ctx.getString(R.string.demand_status_locked)
                    else -> ctx.getString(R.string.demand_status_bugged)
                }
                holder.binding.textHeader.text = string
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            is SagresDemandOffer -> R.layout.item_card_demand_offer_rev
            else -> R.layout.item_demand_header
        }
    }

    private fun buildMergedList(offers: List<SagresDemandOffer> = currentList): List<Any> {
        val list = mutableListOf<Any>()
        val copy = offers.toMutableList()

        Timber.d("List size: ${offers.size}")

        val bugged = copy.filter { !it.selectable }
        copy -= bugged

        Timber.d("Bugged size: ${bugged.size}")

        val current = copy.filter { it.current }
        copy -= current

        val available = offers.filter { it.available }
        copy -= available

        val completed = offers.filter { it.completed }
        copy -= completed

        val locked = offers.filter { it.unavailable }
        copy -= locked

        if (available.isNotEmpty()) {
            list += AvailableHeader
            list.addAll(available)
        }

        if (current.isNotEmpty()) {
            list += CurrentHeader
            list.addAll(current)
        }

        if (locked.isNotEmpty()) {
            list += LockedHeader
            list.addAll(locked)
        }

        if (completed.isNotEmpty()) {
            list += CompletedHeader
            list.addAll(completed)
        }

        if (bugged.isNotEmpty()) {
            list += BuggedHeader
            list.addAll(bugged)
        }

        return list
    }

    override fun getItemCount() = differ.currentList.size
}

sealed class DemandHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class HeaderHolder(val binding: ItemDemandHeaderBinding) : DemandHolder(binding.root)
    class OfferHolder(val binding: ItemCardDemandOfferRevBinding) : DemandHolder(binding.root)
}

private object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === AvailableHeader && newItem === AvailableHeader -> true
            oldItem === CurrentHeader && newItem === CurrentHeader -> true
            oldItem === LockedHeader && newItem === LockedHeader -> true
            oldItem === CompletedHeader && newItem === CompletedHeader -> true
            oldItem === BuggedHeader && newItem === BuggedHeader -> true
            oldItem === BuggedHeader && newItem === BuggedHeader -> true
            oldItem is SagresDemandOffer && newItem is SagresDemandOffer -> oldItem.uid == newItem.uid
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is SagresDemandOffer && newItem is SagresDemandOffer -> oldItem == newItem
            else -> true
        }
    }
}

private object AvailableHeader
private object CurrentHeader
private object LockedHeader
private object CompletedHeader
private object BuggedHeader
