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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.accessors.ClassWithGroups
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentDisciplineSemesterBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.provideActivityViewModel
import java.lang.IllegalStateException
import javax.inject.Inject

class DisciplineSemesterFragment: UFragment(), Injectable {
    companion object {
        const val SEMESTER_SAGRES_ID = "unes_sagres_id"
        const val SEMESTER_DATABASE_ID = "unes_database_id"

        fun newInstance(semester: Semester): DisciplineSemesterFragment {
            val args = bundleOf(SEMESTER_SAGRES_ID to semester.uid, SEMESTER_DATABASE_ID to semester.uid)
            return DisciplineSemesterFragment().apply { arguments = args }
        }
    }

    private val semesterId: Long by lazy {
        val args = arguments?: throw IllegalStateException("Arguments are null")
        args.getLong(SEMESTER_DATABASE_ID)
    }

    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: DisciplineViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DisciplineSemesterAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        val binding = FragmentDisciplineSemesterBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@DisciplineSemesterFragment)
            viewModel = this@DisciplineSemesterFragment.viewModel
        }.also {
            recyclerView = it.disciplinesRecycler
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = DisciplineSemesterAdapter(viewModel)
        recyclerView.apply {
            adapter = this@DisciplineSemesterFragment.adapter
            (layoutManager as LinearLayoutManager).recycleChildrenOnDetach = true
            (itemAnimator as DefaultItemAnimator).run {
                supportsChangeAnimations = false
                addDuration = 160L
                moveDuration = 160L
                changeDuration = 160L
                removeDuration = 120L
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.classes(semesterId).observe(this, Observer {
            val mapped = ArrayList<ClassWithGroups>()

            it.groupBy { cg -> cg.discipline() }.entries.forEach { e ->
                var added = false
                e.value.forEach { cg ->
                    val grs = cg.groups
                    if (grs.isNotEmpty()) {
                        val std = grs[0].students
                        if (std.isNotEmpty()) {
                            val grd = std[0].grades
                            if (grd.isNotEmpty()) {
                                mapped.add(cg)
                                added = true
                            }
                        }
                    }
                }
                if (e.value.isNotEmpty() && !added) mapped.add(e.value[0])
            }
            populateInterface(mapped)
        })
    }

    private fun populateInterface(classes: List<ClassWithGroups>) {
        adapter.submitList(classes)
    }
}