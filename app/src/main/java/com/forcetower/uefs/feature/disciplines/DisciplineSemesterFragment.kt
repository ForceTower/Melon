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

package com.forcetower.uefs.feature.disciplines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentDisciplineSemesterBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import timber.log.Timber
import javax.inject.Inject

class DisciplineSemesterFragment : UFragment(), Injectable {
    companion object {
        const val SEMESTER_SAGRES_ID = "unes_sagres_id"
        const val SEMESTER_DATABASE_ID = "unes_database_id"

        fun newInstance(semester: Semester): DisciplineSemesterFragment {
            val args = bundleOf(SEMESTER_SAGRES_ID to semester.sagresId, SEMESTER_DATABASE_ID to semester.uid)
            return DisciplineSemesterFragment().apply { arguments = args }
        }
    }

    private val semesterId: Long by lazy {
        val args = arguments ?: throw IllegalStateException("Arguments are null")
        args.getLong(SEMESTER_DATABASE_ID)
    }

    private val semesterSagresId: Long by lazy {
        val args = arguments ?: throw IllegalStateException("Arguments are null")
        args.getLong(SEMESTER_SAGRES_ID)
    }

    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: DisciplineViewModel
    private lateinit var localDisciplineVM: DisciplineViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterPerformance: DisciplinePerformanceAdapter
    private lateinit var swipeRefreshLayout: com.forcetower.core.widget.CustomSwipeRefreshLayout
    private lateinit var binding: FragmentDisciplineSemesterBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        localDisciplineVM = provideViewModel(factory)
        binding = FragmentDisciplineSemesterBinding.inflate(inflater, container, false).apply {
            viewModel = this@DisciplineSemesterFragment.viewModel
        }.also {
            recyclerView = it.disciplinesRecycler
            swipeRefreshLayout = it.swipeRefresh
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try { binding.lifecycleOwner = viewLifecycleOwner } catch (t: Throwable) { Timber.e(t) }

        adapterPerformance = DisciplinePerformanceAdapter(viewModel)
        recyclerView.adapter = adapterPerformance

        recyclerView.apply {
            (layoutManager as LinearLayoutManager).recycleChildrenOnDetach = true
            (itemAnimator as DefaultItemAnimator).run {
                supportsChangeAnimations = false
                addDuration = 160L
                moveDuration = 160L
                changeDuration = 160L
                removeDuration = 120L
            }
            setRecycledViewPool(RecyclerView.RecycledViewPool().apply {
                setMaxRecycledViews(4, 7)
                setMaxRecycledViews(8, 15)
            })
        }
        swipeRefreshLayout.setOnRefreshListener {
            localDisciplineVM.updateGradesFromSemester(semesterSagresId)
        }

        binding.downloadBtn.setOnClickListener {
            localDisciplineVM.updateGradesFromSemester(semesterSagresId)
        }

        localDisciplineVM.refreshing.observe(viewLifecycleOwner, {
            swipeRefreshLayout.isRefreshing = it
            binding.loading = it
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.classes(semesterId).observe(viewLifecycleOwner, {
            binding.hasData = it.isNotEmpty()
            populateInterface(it)
        })
    }

    private fun populateInterface(classes: List<ClassFullWithGroup>) {
        adapterPerformance.classes = classes
    }
}