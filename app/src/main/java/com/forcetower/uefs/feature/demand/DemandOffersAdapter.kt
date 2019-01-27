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

package com.forcetower.uefs.feature.demand

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.sagres.database.model.SDemandOffer
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ItemCardDemandOfferBinding
import com.forcetower.uefs.databinding.ItemDemandHeaderBinding
import com.forcetower.uefs.feature.shared.inflate
import timber.log.Timber

class DemandOffersAdapter(
    val lifecycleOwner: LifecycleOwner,
    val viewModel: DemandViewModel
) : RecyclerView.Adapter<DemandHolder>() {
    private val differ = AsyncListDiffer<Any>(this, DiffCallback)

    var currentList: List<SDemandOffer> = listOf()
    set(value) {
        field = value
        differ.submitList(buildMergedList(offers = currentList))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemandHolder {
        return when (viewType) {
            R.layout.item_card_demand_offer -> DemandHolder.OfferHolder(parent.inflate(viewType))
            R.layout.item_demand_header -> DemandHolder.HeaderHolder(parent.inflate(viewType))
            else -> throw IllegalStateException("Unable to inflate $viewType")
        }
    }

    override fun onBindViewHolder(holder: DemandHolder, position: Int) {
        when (holder) {
            is DemandHolder.OfferHolder -> holder.binding.apply {
                offer = differ.currentList[position] as SDemandOffer
                setLifecycleOwner(lifecycleOwner)
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
            is SDemandOffer -> R.layout.item_card_demand_offer
            else -> R.layout.item_demand_header
        }
    }

    private fun buildMergedList(offers: List<SDemandOffer> = currentList): List<Any> {
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
    class OfferHolder(val binding: ItemCardDemandOfferBinding) : DemandHolder(binding.root)
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
            oldItem is SDemandOffer && newItem is SDemandOffer -> oldItem.uid == newItem.uid
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is SDemandOffer && newItem is SDemandOffer -> oldItem == newItem
            else -> true
        }
    }
}

private object AvailableHeader
private object CurrentHeader
private object LockedHeader
private object CompletedHeader
private object BuggedHeader