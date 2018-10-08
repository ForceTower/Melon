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
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.core.storage.database.accessors.ClassStudentWithGroup
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.shared.inflater

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
        val inflater = parent.inflater()
        return when (viewType) {
            R.layout.divider -> OverviewHolder.Divider(inflater.inflate(viewType, parent, false))
            else -> throw IllegalStateException("No view defined for viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: OverviewHolder, position: Int) {

    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            is InfoHeader -> R.layout.item_discipline_info
            is DraftDiscipline -> R.layout.item_discipline_draft_info
            is TeacherSpace -> R.layout.item_discipline_teacher
            is LoadingDiscipline -> R.layout.item_discipline_loading_info
            is MaterialHeader -> R.layout.item_teacher_shared_material_header
            is FrequencyHeader -> R.layout.item_discipline_frequency
            is DisciplineClassHeader -> R.layout.item_discipline_classes_header
            is NoMaterialPosted -> R.layout.item_discipline_no_material_posted
            is NoMissedClasses -> R.layout.item_discipline_no_missed_classes
            is NoClassRegistered -> R.layout.item_discipline_no_class_registered
            is ClassMaterial -> R.layout.item_discipline_class_material
            is ClassAbsence -> R.layout.item_discipline_missed_class
            is ClassItem -> R.layout.item_discipline_class_item
            is Divider -> R.layout.divider
            else -> throw IllegalStateException("No view type defined for position $position")
        }
    }

    private fun buildMergedList(
        discipline: ClassStudentWithGroup? = currentDiscipline,
        frequency: List<ClassAbsence> = frequencyList,
        material: List<ClassMaterial> = materialList,
        items: List<ClassItem> = itemList
    ): List<Any> {
        val list = mutableListOf<Any>()
        list += InfoHeader
        if (discipline != null) {
            list += if (discipline.group().group.draft) DraftDiscipline else TeacherSpace
        }

        if (material.isNotEmpty()) {
            list += MaterialHeader
            list += material[0]
        } else {
            list += NoMaterialPosted
        }

        list += FrequencyHeader
        list += if (frequency.isNotEmpty()) frequency[0] else NoMissedClasses

        list += DisciplineClassHeader
        list += if (items.isNotEmpty()) items[0] else NoClassRegistered
        return list
    }

    private val differ = AsyncListDiffer<Any>(this, DiffCallback)
}

sealed class OverviewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    class Divider(itemView: View): OverviewHolder(itemView)
}

private object DiffCallback: DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === InfoHeader && newItem === InfoHeader -> true
            oldItem === DraftDiscipline && newItem === DraftDiscipline -> true
            oldItem === TeacherSpace && newItem === TeacherSpace -> true
            oldItem === LoadingDiscipline && newItem === LoadingDiscipline -> true
            oldItem === MaterialHeader && newItem === MaterialHeader -> true
            oldItem === FrequencyHeader && newItem === FrequencyHeader -> true
            oldItem === DisciplineClassHeader && newItem === DisciplineClassHeader -> true
            oldItem === NoMaterialPosted && newItem === NoMaterialPosted -> true
            oldItem === NoMissedClasses && newItem === NoMissedClasses -> true
            oldItem === NoClassRegistered && newItem === NoClassRegistered -> true
            oldItem === Divider && newItem === Divider -> true
            oldItem is ClassMaterial && newItem is ClassMaterial -> oldItem.uid == newItem.uid
            oldItem is ClassAbsence && newItem is ClassAbsence -> oldItem.uid == newItem.uid
            oldItem is ClassItem && newItem is ClassItem -> oldItem.uid == newItem.uid
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is ClassMaterial && newItem is ClassMaterial -> oldItem == newItem
            oldItem is ClassAbsence && newItem is ClassAbsence -> oldItem == newItem
            oldItem is ClassItem && newItem is ClassItem -> oldItem == newItem
            else -> true
        }
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
private object Divider