/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.accessors.ClassWithGroups
import com.forcetower.uefs.core.util.toJson
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentDisciplineBinding
import com.forcetower.uefs.feature.disciplines.dialog.SelectGroupDialog
import com.forcetower.uefs.feature.disciplines.disciplinedetail.DisciplineDetailsActivity
import com.forcetower.uefs.feature.home.HomeViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.makeSemester
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import timber.log.Timber
import javax.inject.Inject

class DisciplineFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var preferences: SharedPreferences

    private lateinit var viewModel: DisciplineViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var binding: FragmentDisciplineBinding

    private lateinit var viewPager: ViewPager
    private lateinit var tabs: TabLayout
    private lateinit var adapter: SemesterAdapter

    private var sortedSizeOnce: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        homeViewModel = provideActivityViewModel(factory)
        return FragmentDisciplineBinding.inflate(inflater, container, false).also {
            binding = it
            viewPager = it.pagerSemester
            tabs = it.tabLayout
        }.apply {
            lifecycleOwner = this@DisciplineFragment
            viewModel = this@DisciplineFragment.viewModel
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SemesterAdapter(childFragmentManager)
        viewPager.adapter = adapter
        tabs.setupWithViewPager(viewPager)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.semesters.observe(this, Observer {
            val actualList = applySortOptions(it)
            adapter.submitList(actualList)
        })

        viewModel.navigateToDisciplineAction.observe(this, EventObserver {
            handleNavigateToDisciplineDetails(it)
        })

        viewModel.navigateToGroupAction.observe(this, EventObserver {
            startActivity(DisciplineDetailsActivity.startIntent(requireContext(), it.classId, it.uid))
        })
    }

    private fun applySortOptions(semesters: List<Semester>): List<Semester> {
        val ordering = preferences.getBoolean("stg_semester_deterministic_ordering", true)
        val diffSort = semesters.sorted()
        val size = semesters.size

        if (ordering && sortedSizeOnce != size) {
            val suggestedOrdering = preferences.getBoolean("suggested_reordering_of_semesters", false)
            if (!suggestedOrdering && diffSort != semesters) {
                sortedSizeOnce = size
                preferences.edit().putBoolean("suggested_reordering_of_semesters", true).apply()
                val snack = getSnack(getString(R.string.incorrect_semester_ordering_detected), true)
                snack?.let { bar ->
                    bar.duration = Snackbar.LENGTH_INDEFINITE
                    bar.setAction(getString(R.string.incorrect_semester_ordering_quick_fix)) {
                        preferences.edit().putBoolean("stg_semester_deterministic_ordering", false).apply()
                        bar.dismiss()
                        showAppliedChangesSnack()
                    }

                    bar.show()
                }
            }
        }

        return if (ordering) {
            semesters
        } else {
            diffSort
        }
    }

    private fun showAppliedChangesSnack() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                val snack = getSnack(getString(R.string.incorrect_semester_ordering_changes_applied))
                snack?.let { bar ->
                    bar.duration = Snackbar.LENGTH_LONG
                    bar.setAction(getString(R.string.incorrect_semester_ordering_undo_changes)) {
                        preferences.edit().putBoolean("stg_semester_deterministic_ordering", true).apply()
                        bar.dismiss()
                    }
                    bar.show()
                }
            } else {
                Timber.d("Failed check of state")
            }
        }, 1000)
    }

    private fun handleNavigateToDisciplineDetails(it: ClassWithGroups) {
        when {
            it.groups.isEmpty() -> homeViewModel.showSnack(getString(R.string.no_class_groups))
            it.groups.size == 1 -> startActivity(DisciplineDetailsActivity.startIntent(requireContext(), it.clazz.uid, it.groups[0].uid))
            else -> showGroupDialog(it)
        }
    }

    private fun showGroupDialog(it: ClassWithGroups) {
        val dialog = SelectGroupDialog().apply {
            arguments = bundleOf("groups" to it.toJson())
        }
        dialog.show(childFragmentManager, "select_discipline_group")
    }

    private inner class SemesterAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val semesters: MutableList<Semester> = ArrayList()

        fun submitList(list: List<Semester>) {
            semesters.clear()
            semesters.addAll(list)
            notifyDataSetChanged()
        }

        override fun getCount() = semesters.size
        override fun getItem(position: Int) = DisciplineSemesterFragment.newInstance(semesters[position])
        override fun getPageTitle(position: Int) = semesters[position].codename.makeSemester()
    }
}