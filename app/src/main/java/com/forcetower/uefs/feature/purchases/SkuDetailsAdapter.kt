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
            executePendingBindings()
        }
    }

    inner class SkuHolder(val binding: ItemSkuDetailsBinding) : RecyclerView.ViewHolder(binding.root)

    object SkuDiff : DiffUtil.ItemCallback<SkuDetails>() {
        override fun areItemsTheSame(oldItem: SkuDetails, newItem: SkuDetails) = oldItem.sku == newItem.sku
        override fun areContentsTheSame(oldItem: SkuDetails, newItem: SkuDetails) = oldItem == newItem
    }
}