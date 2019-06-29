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

package com.forcetower.uefs.feature.disciplines.disciplinedetail.materials

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.databinding.ItemDisciplineClassMaterialBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.shared.inflate

class MaterialAdapter(
    val lifecycleOwner: LifecycleOwner,
    val viewModel: DisciplineViewModel
) : ListAdapter<ClassMaterial, MaterialAdapter.ClassMaterialHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ClassMaterialHolder(parent.inflate(R.layout.item_discipline_class_material))

    override fun onBindViewHolder(holder: MaterialAdapter.ClassMaterialHolder, position: Int) {
        holder.binding.apply {
            material = getItem(position)
            listener = viewModel
            lifecycleOwner = lifecycleOwner
            executePendingBindings()
        }
    }

    inner class ClassMaterialHolder(val binding: ItemDisciplineClassMaterialBinding) : RecyclerView.ViewHolder(binding.root)

    private object DiffCallback : DiffUtil.ItemCallback<ClassMaterial>() {
        override fun areItemsTheSame(oldItem: ClassMaterial, newItem: ClassMaterial) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: ClassMaterial, newItem: ClassMaterial) = oldItem == newItem
    }
}
