/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package dev.forcetower.disciplines.feature

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.core.os.bundleOf
import androidx.core.view.postDelayed
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.core.layout.JumpSmoothScroller
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.injection.dependencies.DisciplineModuleDependencies
import com.forcetower.uefs.core.model.ui.disciplines.CheckableSemester
import com.forcetower.uefs.core.model.ui.disciplines.DisciplinesDataUI
import com.forcetower.uefs.core.model.ui.disciplines.DisciplinesIndexed
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup
import com.forcetower.uefs.core.util.toJson
import com.forcetower.uefs.feature.disciplines.disciplinedetail.DisciplineDetailsActivity
import com.forcetower.uefs.feature.home.HomeViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.clearDecorations
import com.forcetower.uefs.widget.BubbleDecoration
import com.google.android.play.core.splitcompat.SplitCompat
import dagger.hilt.android.EntryPointAccessors
import dev.forcetower.disciplines.core.injection.DaggerDisciplineComponent
import dev.forcetower.disciplines.databinding.FragmentDisciplineBinding
import dev.forcetower.disciplines.feature.dialog.SelectGroupDialog
import timber.log.Timber
import javax.inject.Inject

@Keep
class DisciplineFragment : UFragment() {
    @Inject lateinit var factory: ViewModelProvider.Factory
    private val viewModel: DisciplinesViewModel by activityViewModels { factory }
    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var semesterIndicatorItemDecoration: BubbleDecoration
    private lateinit var binding: FragmentDisciplineBinding

    private lateinit var semesterAdapter: DisciplineSemesterAdapter
    private lateinit var disciplineAdapter: DisciplinePerformanceAdapter
    private lateinit var disciplinesIndexed: DisciplinesIndexed
    private var cachedBubbleRange: IntRange? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        SplitCompat.install(context)
        DaggerDisciplineComponent.builder()
            .context(context)
            .dependencies(
                EntryPointAccessors.fromActivity(
                    requireActivity(),
                    DisciplineModuleDependencies::class.java
                )
            )
            .build()
            .inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = FragmentDisciplineBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            lifecycleOwner = viewLifecycleOwner
            actions = viewModel
        }.root

        semesterIndicatorItemDecoration = BubbleDecoration(requireContext())
        binding.semesterIndicators.addItemDecoration(semesterIndicatorItemDecoration)

        disciplineAdapter = DisciplinePerformanceAdapter(viewModel)
        semesterAdapter = DisciplineSemesterAdapter(viewModel)

        binding.disciplinesRecycler.apply {
            adapter = disciplineAdapter
            (itemAnimator as DefaultItemAnimator).run {
                supportsChangeAnimations = false
                addDuration = 160L
                moveDuration = 160L
                changeDuration = 160L
                removeDuration = 120L
            }

            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recycler: RecyclerView, dx: Int, dy: Int) {
                        onDisciplinesScrolled()
                    }
                }
            )
        }

        binding.semesterIndicators.apply {
            adapter = semesterAdapter
            (itemAnimator as DefaultItemAnimator).run {
                supportsChangeAnimations = false
                addDuration = 0L
                moveDuration = 0L
                changeDuration = 0L
                removeDuration = 0L
            }
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        semesterIndicatorItemDecoration.userScrolled = true
                    }
                }
            )
        }

        return view
    }

    private fun onDisciplinesScrolled() {
        val manager = (binding.disciplinesRecycler.layoutManager) as LinearLayoutManager
        val first = manager.findFirstVisibleItemPosition()
        val last = manager.findLastVisibleItemPosition()
        if (first < 0 || last < 0) {
            return
        }

        val firstSemester = disciplinesIndexed.semesterForPosition(first) ?: return
        val lastSemester = disciplinesIndexed.semesterForPosition(last) ?: return
        val highlightRange = disciplinesIndexed.semesters.indexOf(firstSemester)..disciplinesIndexed.semesters.indexOf(lastSemester)
        if (highlightRange != cachedBubbleRange) {
            cachedBubbleRange = highlightRange
            buildSemesterIndicators(disciplinesIndexed.semesters)
            binding.semesterIndicators.postDelayed(500) {
//                scrollAdapterToPosition(firstSemester)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.disciplines.observe(
            viewLifecycleOwner,
            {
                Timber.d("Discipline Data sized ${it.data.size}")
                onUiUpdate(it)
            }
        )

        viewModel.scrollToEvent.observe(
            viewLifecycleOwner,
            EventObserver { scrollEvent ->
                if (scrollEvent.targetPosition != -1) {
                    binding.disciplinesRecycler.run {
                        post {
                            val lm = layoutManager as LinearLayoutManager
                            if (scrollEvent.smoothScroll) {
                                val scroller = JumpSmoothScroller(requireContext())
                                scroller.targetPosition = scrollEvent.targetPosition
                                lm.startSmoothScroll(scroller)
                            } else {
                                lm.scrollToPositionWithOffset(scrollEvent.targetPosition, 0)
                            }
                        }
                    }
                }
            }
        )

        viewModel.navigateToDisciplineAction.observe(
            viewLifecycleOwner,
            EventObserver {
                handleNavigateToDisciplineDetails(it)
            }
        )

        viewModel.navigateToGroupAction.observe(
            viewLifecycleOwner,
            EventObserver {
                startActivity(DisciplineDetailsActivity.startIntent(requireContext(), it.classId, it.uid))
            }
        )

        binding.textToolbarTitle.setOnClickListener {
            (requireContext().applicationContext as UApplication).disciplineToolbarDevClickCount++
        }
    }

    private fun onUiUpdate(values: DisciplinesDataUI) {
        disciplineAdapter.submitList(values.data)

        val indexed = values.indexer
        disciplinesIndexed = indexed

        cachedBubbleRange = null

        if (indexed.semesters.isEmpty()) {
            cachedBubbleRange = -1..-1
            buildSemesterIndicators(indexed.semesters)
        }

        binding.disciplinesRecycler.run {
            clearDecorations()
            if (values.data.isNotEmpty()) {
                addItemDecoration(
                    SemestersDisciplineSeparatorItemDecoration(
                        context,
                        indexed
                    )
                )
            }
        }
    }

    private fun buildSemesterIndicators(semesters: List<Semester>) {
        val bubbleRange = cachedBubbleRange ?: return
        val mapped = semesters.mapIndexed { index, semester ->
            CheckableSemester(semester, index in bubbleRange)
        }
        semesterAdapter.submitList(mapped)
        semesterIndicatorItemDecoration.bubbleRange = bubbleRange
    }

    private fun handleNavigateToDisciplineDetails(it: ClassFullWithGroup) {
        when {
            it.groups.isEmpty() -> homeViewModel.showSnack(getString(R.string.no_class_groups))
            it.groups.size == 1 -> startActivity(DisciplineDetailsActivity.startIntent(requireContext(), it.clazz.uid, it.groups[0].uid))
            else -> showGroupDialog(it)
        }
    }

    private fun showGroupDialog(it: ClassFullWithGroup) {
        val dialog = SelectGroupDialog().apply {
            arguments = bundleOf("groups" to it.toJson())
        }
        dialog.show(childFragmentManager, "select_discipline_group")
    }
}
