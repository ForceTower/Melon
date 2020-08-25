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

    override fun onBindViewHolder(holder: ClassMaterialHolder, position: Int) {
        holder.binding.apply {
            material = getItem(position)
            listener = viewModel
            lifecycleOwner = this@MaterialAdapter.lifecycleOwner
            executePendingBindings()
        }
    }

    inner class ClassMaterialHolder(val binding: ItemDisciplineClassMaterialBinding) : RecyclerView.ViewHolder(binding.root)

    private object DiffCallback : DiffUtil.ItemCallback<ClassMaterial>() {
        override fun areItemsTheSame(oldItem: ClassMaterial, newItem: ClassMaterial) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: ClassMaterial, newItem: ClassMaterial) = oldItem == newItem
    }
}
