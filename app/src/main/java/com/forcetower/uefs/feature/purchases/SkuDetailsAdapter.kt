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

package com.forcetower.uefs.feature.purchases

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.SkuDetails
import com.forcetower.uefs.R
import com.forcetower.uefs.core.vm.BillingViewModel
import com.forcetower.uefs.databinding.ItemSkuDetailsBinding
import com.forcetower.uefs.feature.shared.inflate

class SkuDetailsAdapter(
    private val viewModel: BillingViewModel
) : ListAdapter<SkuDetails, SkuDetailsAdapter.SkuHolder>(SkuDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkuHolder {
        return SkuHolder(parent.inflate(R.layout.item_sku_details))
    }

    override fun onBindViewHolder(holder: SkuHolder, position: Int) {
        holder.binding.apply {
            skuDetails = getItem(position)
            viewModel = this@SkuDetailsAdapter.viewModel
            executePendingBindings()
        }
    }

    inner class SkuHolder(val binding: ItemSkuDetailsBinding) : RecyclerView.ViewHolder(binding.root)

    object SkuDiff : DiffUtil.ItemCallback<SkuDetails>() {
        override fun areItemsTheSame(oldItem: SkuDetails, newItem: SkuDetails) = oldItem.sku == newItem.sku
        override fun areContentsTheSame(oldItem: SkuDetails, newItem: SkuDetails) = oldItem == newItem
    }
}
