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

package com.forcetower.uefs.feature.disciplines

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.storage.database.accessors.ClassWithGroups
import com.forcetower.uefs.databinding.ItemDisciplineCollapsedBinding
import com.forcetower.uefs.feature.shared.inflater

private const val DISCIPLINE = 4

class DisciplineSemesterAdapter(
    val viewModel: DisciplineViewModel
) : ListAdapter<ClassWithGroups, ClassHolder>(ClassDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassHolder {
        val binding = ItemDisciplineCollapsedBinding.inflate(parent.inflater(), parent, false)
        return ClassHolder(binding)
    }

    override fun onBindViewHolder(holder: ClassHolder, position: Int) {
        holder.binding.apply {
            clazzGroup = getItem(position)
            listener = viewModel
            executePendingBindings()
        }
    }

    override fun getItemViewType(position: Int) = DISCIPLINE
}

class ClassHolder(val binding: ItemDisciplineCollapsedBinding) : RecyclerView.ViewHolder(binding.root)

private object ClassDiff : DiffUtil.ItemCallback<ClassWithGroups>() {
    override fun areItemsTheSame(oldItem: ClassWithGroups, newItem: ClassWithGroups) = oldItem.clazz.uid == newItem.clazz.uid && oldItem.discipline().uid == newItem.discipline().uid
    override fun areContentsTheSame(oldItem: ClassWithGroups, newItem: ClassWithGroups) = oldItem == newItem
}

