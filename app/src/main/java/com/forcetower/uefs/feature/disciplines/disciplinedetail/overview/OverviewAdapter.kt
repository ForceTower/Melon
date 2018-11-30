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

package com.forcetower.uefs.feature.disciplines.disciplinedetail.overview

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.storage.database.accessors.ClassFullWithGroup
import com.forcetower.uefs.databinding.ItemDisciplineGoalsBinding
import com.forcetower.uefs.databinding.ItemDisciplineShortBinding
import com.forcetower.uefs.databinding.ItemDisciplineTeacherBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.shared.inflate
import com.forcetower.uefs.feature.shared.inflater

class OverviewAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: DisciplineViewModel
) : RecyclerView.Adapter<OverviewHolder>() {
    var currentClazz: ClassFullWithGroup? = null
    set(value) {
        field = value
        differ.submitList(buildMergedList(clazz = value))
    }

    var currentGroup: ClassGroup? = null
    set(value) {
        field = value
        differ.submitList(buildMergedList(group = value))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverviewHolder {
        val inflater = parent.inflater()
        return when (viewType) {
            R.layout.item_discipline_short -> OverviewHolder.ShortHolder(
                parent.inflate(viewType)
            )
            R.layout.item_discipline_teacher -> OverviewHolder.TeacherHolder(
                parent.inflate(viewType)
            )
            R.layout.item_discipline_goals -> OverviewHolder.ResumeHolder(
                parent.inflate(viewType)
            )
            else -> OverviewHolder.SimpleHolder(
                inflater.inflate(viewType, parent, false)
            ) /* Draft or Statistics */
        }
    }

    override fun onBindViewHolder(holder: OverviewHolder, position: Int) {
        when (holder) {
            is OverviewHolder.SimpleHolder -> Unit
            is OverviewHolder.ShortHolder -> holder.binding.apply {
                setLifecycleOwner(lifecycleOwner)
                viewModel = this@OverviewAdapter.viewModel
                executePendingBindings()
            }
            is OverviewHolder.TeacherHolder -> holder.binding.apply {
                setLifecycleOwner(lifecycleOwner)
                viewModel = this@OverviewAdapter.viewModel
                executePendingBindings()
            }
            is OverviewHolder.ResumeHolder -> holder.binding.apply {
                setLifecycleOwner(lifecycleOwner)
                viewModel = this@OverviewAdapter.viewModel
                executePendingBindings()
            }
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            is DisciplineDraft -> R.layout.item_discipline_draft_info
            is DisciplineShort -> R.layout.item_discipline_short
            is DisciplineTeacher -> R.layout.item_discipline_teacher
            is DisciplineResume -> R.layout.item_discipline_goals
            is Statistics -> R.layout.item_discipline_statistics_short
            else -> throw IllegalStateException("No view type defined for position $position")
        }
    }

    private fun buildMergedList(
        clazz: ClassFullWithGroup? = currentClazz,
        group: ClassGroup? = currentGroup
    ): List<Any> {
        val list = mutableListOf<Any>()
        if (clazz != null) {

            if (group?.draft == false) {
                list += DisciplineDraft
            }

            if (clazz.clazz.discipline().shortText != null) {
                list += DisciplineShort
            }

            if (group?.teacher != null) {
                list += DisciplineTeacher
            }

            if (clazz.clazz.discipline().resume != null) {
                list += DisciplineResume
            }

            list += Statistics
        }
        return list
    }

    private val differ = AsyncListDiffer<Any>(this,
        DiffCallback
    )
}

sealed class OverviewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class SimpleHolder(itemView: View) : OverviewHolder(itemView)
    class ShortHolder(val binding: ItemDisciplineShortBinding) : OverviewHolder(binding.root)
    class TeacherHolder(val binding: ItemDisciplineTeacherBinding) : OverviewHolder(binding.root)
    class ResumeHolder(val binding: ItemDisciplineGoalsBinding) : OverviewHolder(binding.root)
}

private object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === DisciplineDraft && newItem === DisciplineDraft -> true
            oldItem === DisciplineShort && newItem === DisciplineShort -> true
            oldItem === DisciplineTeacher && newItem === DisciplineTeacher -> true
            oldItem === DisciplineResume && newItem === DisciplineResume -> true
            oldItem === Statistics && newItem === Statistics -> true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return true
    }
}

private object DisciplineDraft
private object DisciplineShort
private object DisciplineTeacher
private object DisciplineResume
private object Statistics
