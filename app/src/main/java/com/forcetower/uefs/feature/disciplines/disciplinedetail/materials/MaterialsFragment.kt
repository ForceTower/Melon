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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.forcetower.uefs.databinding.FragmentDisciplineAttachmentsBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.disciplines.disciplinedetail.DisciplineDetailsActivity
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MaterialsFragment : UFragment() {
    private val viewModel: DisciplineViewModel by activityViewModels()
    private lateinit var binding: FragmentDisciplineAttachmentsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentDisciplineAttachmentsBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val materialsAdapter = MaterialAdapter(this, viewModel)
        binding.attachmentsRecycler.apply {
            adapter = materialsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
        }

        viewModel.materials.observe(
            viewLifecycleOwner,
            Observer {
                materialsAdapter.submitList(it)
                if (it.isEmpty()) {
                    binding.layoutNoData.visibility = VISIBLE
                    binding.attachmentsRecycler.visibility = GONE
                } else {
                    binding.layoutNoData.visibility = GONE
                    binding.attachmentsRecycler.visibility = VISIBLE
                }
            }
        )
    }

    companion object {
        fun newInstance(classId: Long): MaterialsFragment {
            return MaterialsFragment().apply {
                arguments = bundleOf(DisciplineDetailsActivity.CLASS_GROUP_ID to classId)
            }
        }
    }
}
