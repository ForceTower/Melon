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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentDisciplineDetailsBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.disciplines.disciplinedetail.classes.ClassesFragment
import com.forcetower.uefs.feature.disciplines.disciplinedetail.overview.OverviewFragment
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.provideActivityViewModel
import com.google.android.material.tabs.TabLayout
import javax.inject.Inject

class DisciplineDetailsFragment: UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: DisciplineViewModel
    private lateinit var binding: FragmentDisciplineDetailsBinding
    private lateinit var tabs: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var adapter: DetailsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDisciplineDetailsBinding.inflate(inflater, container, false).also {
            viewPager = it.viewPager
            tabs = it.tabs
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = DetailsAdapter(childFragmentManager)
        viewPager.adapter = adapter
        tabs.setupWithViewPager(viewPager)
        viewPager.offscreenPageLimit = 4
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = provideActivityViewModel(factory)
        viewModel.setClassGroupId(requireNotNull(arguments).getLong(DisciplineDetailsActivity.CLASS_GROUP_ID))
        binding.apply {
            viewModel = this@DisciplineDetailsFragment.viewModel
            setLifecycleOwner(this@DisciplineDetailsFragment)
        }

        createFragments()
    }

    private fun createFragments() {
        val group = requireNotNull(arguments).getLong(DisciplineDetailsActivity.CLASS_GROUP_ID)
        val overview = getString(R.string.discipline_details_overview) to OverviewFragment.newInstance(group)
        val grades = getString(R.string.discipline_details_grades) to GradesFragment.newInstance(group)
        val classes = getString(R.string.discipline_details_classes) to ClassesFragment.newInstance(group)
        val materials = getString(R.string.discipline_details_materials) to MaterialsFragment.newInstance(group)
        val list = listOf<Pair<String, Fragment>>(overview, grades, classes, materials)
        adapter.submitList(list)
    }

    private inner class DetailsAdapter(fm: FragmentManager): FragmentPagerAdapter(fm) {
        private val tabs = mutableListOf<Pair<String, Fragment>>()
        override fun getItem(position: Int) = tabs[position].second
        override fun getCount() = tabs.size
        override fun getPageTitle(position: Int) = tabs[position].first
        fun submitList(pairs: List<Pair<String, Fragment>>) {
            tabs.clear()
            tabs.addAll(pairs)
            notifyDataSetChanged()
        }
    }


    companion object {
        fun newInstance(classId: Long): DisciplineDetailsFragment {
            return DisciplineDetailsFragment().apply {
                arguments = bundleOf(DisciplineDetailsActivity.CLASS_GROUP_ID to classId)
            }
        }
    }
}