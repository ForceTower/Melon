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

package com.forcetower.uefs.feature.grades

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.databinding.ItemGradeBinding
import com.forcetower.uefs.feature.common.DisciplineActions
import com.forcetower.uefs.feature.shared.inflater

private const val GRADE = 8

class ClassGroupGradesAdapter(
    private val listener: DisciplineActions?
) : ListAdapter<Grade, GradesHolder>(GradesDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradesHolder {
        val binding = ItemGradeBinding.inflate(parent.inflater(), parent, false)
        return GradesHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: GradesHolder, position: Int) {
        holder.binding.apply {
            val item = getItem(position)
            grade = item
            executePendingBindings()
        }
    }

    override fun getItemViewType(position: Int) = GRADE
}

class GradesHolder(val binding: ItemGradeBinding, listener: DisciplineActions?) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.listener = listener
    }
}

private object GradesDiff : DiffUtil.ItemCallback<Grade>() {
    override fun areItemsTheSame(oldItem: Grade, newItem: Grade) = oldItem.uid == newItem.uid
    override fun areContentsTheSame(oldItem: Grade, newItem: Grade) = oldItem == newItem
}