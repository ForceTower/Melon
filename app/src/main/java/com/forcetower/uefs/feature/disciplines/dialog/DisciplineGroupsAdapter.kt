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

package com.forcetower.uefs.feature.disciplines.dialog

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.databinding.ItemSelectDisciplineGroupBinding
import com.forcetower.uefs.feature.common.DisciplineActions
import com.forcetower.uefs.feature.shared.inflate

class DisciplineGroupsAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val listener: DisciplineActions
): ListAdapter<ClassGroup, GroupHolder>(GroupDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = GroupHolder(
        parent.inflate(R.layout.item_select_discipline_group)
    )

    override fun onBindViewHolder(holder: GroupHolder, position: Int) {
        holder.binding.apply {
            group = getItem(position)
            listener = this@DisciplineGroupsAdapter.listener
            setLifecycleOwner(this@DisciplineGroupsAdapter.lifecycleOwner)
            executePendingBindings()
        }
    }
}

class GroupHolder(val binding: ItemSelectDisciplineGroupBinding): RecyclerView.ViewHolder(binding.root)

private object GroupDiff: DiffUtil.ItemCallback<ClassGroup>() {
    override fun areItemsTheSame(oldItem: ClassGroup, newItem: ClassGroup) = oldItem.uid == newItem.uid
    override fun areContentsTheSame(oldItem: ClassGroup, newItem: ClassGroup) = oldItem == newItem
}