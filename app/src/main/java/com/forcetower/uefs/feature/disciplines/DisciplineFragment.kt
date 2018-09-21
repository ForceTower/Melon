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
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.vm.DisciplineViewModel
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentDisciplineBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.makeSemester
import com.forcetower.uefs.feature.shared.provideActivityViewModel
import com.google.android.material.tabs.TabLayout
import javax.inject.Inject

class DisciplineFragment: UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: DisciplineViewModel
    private lateinit var binding: FragmentDisciplineBinding

    private lateinit var viewPager: ViewPager
    private lateinit var tabs: TabLayout
    private lateinit var adapter: SemesterAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        return FragmentDisciplineBinding.inflate(inflater, container, false).also {
            binding = it
            viewPager = it.pagerSemester
            tabs = it.tabLayout
        }.apply {
            setLifecycleOwner(this@DisciplineFragment)
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
            adapter.submitList(it)
        })
    }

    private inner class SemesterAdapter(fm: FragmentManager):  FragmentPagerAdapter(fm) {
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