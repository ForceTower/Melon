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

package com.forcetower.uefs.feature.disciplines.disciplinedetail

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.core.storage.database.accessors.ClassStudentWithGroup
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel

class OverviewAdapter(
    lifecycleOwner: LifecycleOwner,
    viewModel: DisciplineViewModel
): RecyclerView.Adapter<OverviewHolder>() {
    var currentDiscipline: ClassStudentWithGroup? = null
    set(value) {
        field = value
        differ.submitList(buildMergedList(discipline = value))
    }

    var frequencyList: List<ClassAbsence> = emptyList()
    set(value) {
        field = value
        differ.submitList(buildMergedList(frequency = value))
    }

    var materialList: List<ClassMaterial> = emptyList()
    set(value) {
        field = value
        differ.submitList(buildMergedList(material = value))
    }

    var itemList: List<ClassItem> = emptyList()
    set(value) {
        field = value
        differ.submitList(buildMergedList(items = value))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverviewHolder {

    }

    override fun onBindViewHolder(holder: OverviewHolder, position: Int) {

    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int {

    }

    private fun buildMergedList(
        discipline: ClassStudentWithGroup? = currentDiscipline,
        frequency: List<ClassAbsence> = frequencyList,
        material: List<ClassMaterial> = materialList,
        items: List<ClassItem> = itemList
    ): List<Any> {
        val list = mutableListOf<Any>()
        if (discipline != null) {
            list += InfoHeader
            list += if (discipline.group().group.draft) DraftDiscipline else TeacherSpace
        } else {
            list += LoadingDiscipline
        }

        list += MaterialHeader
        list += if (material.isNotEmpty()) material[0] else NoMaterialPosted

        list += FrequencyHeader
        list += if (frequency.isNotEmpty()) frequency[0] else NoMissedClasses

        list += DisciplineClassHeader
        list += if (items.isNotEmpty()) items[0] else NoClassRegistered
        return list
    }

    private val differ = AsyncListDiffer<Any>(this, DiffCallback)
}

sealed class OverviewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

}

private object DiffCallback: DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {

    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {

    }
}

private object LoadingDiscipline
private object InfoHeader
private object TeacherSpace
private object FrequencyHeader
private object MaterialHeader
private object DraftDiscipline
private object NoMissedClasses
private object NoMaterialPosted
private object DisciplineClassHeader
private object NoClassRegistered