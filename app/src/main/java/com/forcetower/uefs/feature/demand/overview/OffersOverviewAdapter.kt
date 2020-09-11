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

package com.forcetower.uefs.feature.demand.overview

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.sagres.database.model.SagresDemandOffer
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ItemDemandOfferBinding
import com.forcetower.uefs.feature.demand.DemandViewModel
import com.forcetower.uefs.feature.shared.inflate

class OffersOverviewAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: DemandViewModel
) : ListAdapter<SagresDemandOffer, DemandHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemandHolder {
        return DemandHolder(parent.inflate(R.layout.item_demand_offer))
    }

    override fun onBindViewHolder(holder: DemandHolder, position: Int) {
        holder.binding.apply {
            lifecycleOwner = this@OffersOverviewAdapter.lifecycleOwner
            offer = getItem(position)
            actions = viewModel
            executePendingBindings()
        }
    }
}

class DemandHolder(val binding: ItemDemandOfferBinding) : RecyclerView.ViewHolder(binding.root)

object DiffCallback : DiffUtil.ItemCallback<SagresDemandOffer>() {
    override fun areItemsTheSame(oldItem: SagresDemandOffer, newItem: SagresDemandOffer) = oldItem.uid == newItem.uid
    override fun areContentsTheSame(oldItem: SagresDemandOffer, newItem: SagresDemandOffer) = oldItem == newItem
}
