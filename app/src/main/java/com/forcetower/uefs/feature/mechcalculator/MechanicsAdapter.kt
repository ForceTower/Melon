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

package com.forcetower.uefs.feature.mechcalculator

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ItemMechValueBinding
import com.forcetower.uefs.feature.shared.inflate

class MechanicsAdapter(
    val lifecycleOwner: LifecycleOwner,
    val viewModel: MechanicalViewModel
) : ListAdapter<MechValue, MechanicsAdapter.MechHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MechHolder(
        parent.inflate(R.layout.item_mech_value)
    )

    override fun onBindViewHolder(holder: MechHolder, position: Int) {
        holder.binding.run {
            item = getItem(position)
            interactor = viewModel
            lifecycleOwner = this@MechanicsAdapter.lifecycleOwner
            executePendingBindings()
        }
    }

    inner class MechHolder(val binding: ItemMechValueBinding) : RecyclerView.ViewHolder(binding.root)
    private object DiffCallback : DiffUtil.ItemCallback<MechValue>() {
        override fun areItemsTheSame(oldItem: MechValue, newItem: MechValue) = oldItem.uuid == newItem.uuid
        override fun areContentsTheSame(oldItem: MechValue, newItem: MechValue) = oldItem == newItem
    }
}
