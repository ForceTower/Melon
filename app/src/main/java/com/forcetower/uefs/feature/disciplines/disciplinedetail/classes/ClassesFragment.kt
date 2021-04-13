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

package com.forcetower.uefs.feature.disciplines.disciplinedetail.classes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.databinding.FragmentDisciplineClassesBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.disciplines.dialog.SelectMaterialDialog
import com.forcetower.uefs.feature.disciplines.disciplinedetail.DisciplineDetailsActivity
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClassesFragment : UFragment() {
    private val viewModel: DisciplineViewModel by activityViewModels()
    private lateinit var binding: FragmentDisciplineClassesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentDisciplineClassesBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val classesAdapter = ClassesAdapter(viewModel)

        binding.classesRecycler.apply {
            adapter = classesAdapter
            itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        viewModel.classItems.observe(
            viewLifecycleOwner,
            Observer {
                classesAdapter.submitList(it)
                if (it.isEmpty()) {
                    binding.layoutNoData.visibility = View.VISIBLE
                    binding.classesRecycler.visibility = View.GONE
                } else {
                    binding.layoutNoData.visibility = View.GONE
                    binding.classesRecycler.visibility = View.VISIBLE
                }
            }
        )

        viewModel.classItemClick.observe(
            viewLifecycleOwner,
            EventObserver {
                onOpenClassItemSelector(it)
            }
        )
    }

    private fun onOpenClassItemSelector(item: ClassItem) {
        if (item.numberOfMaterials <= 0) return
        val dialog = SelectMaterialDialog().apply {
            arguments = bundleOf("class_id" to item.uid)
        }
        dialog.show(childFragmentManager, "select_class_material_dialog")
    }

    companion object {
        fun newInstance(classId: Long): ClassesFragment {
            return ClassesFragment().apply {
                arguments = bundleOf(DisciplineDetailsActivity.CLASS_GROUP_ID to classId)
            }
        }
    }
}
