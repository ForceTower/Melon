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

package com.forcetower.unes.feature.siecomp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.storage.resource.Status
import com.forcetower.unes.core.vm.EventViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.FragmentSiecompScheduleBinding
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.provideActivityViewModel
import com.forcetower.unes.feature.siecomp.ETimeUtils.SIECOMPDays
import com.forcetower.unes.feature.siecomp.day.EScheduleDayFragment
import com.google.android.material.tabs.TabLayout
import timber.log.Timber
import javax.inject.Inject

class EScheduleFragment: UFragment(), Injectable {

    companion object {
        private val COUNT = SIECOMPDays.size
    }

    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: EventViewModel
    private lateinit var binding: FragmentSiecompScheduleBinding

    private lateinit var viewPager: ViewPager
    private lateinit var tabs: TabLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)

        return FragmentSiecompScheduleBinding.inflate(inflater, container, false).also {
            binding = it
            tabs = binding.tabLayout
            viewPager = binding.pagerSchedule
        }.apply {
            setLifecycleOwner(this@EScheduleFragment)
            viewModel = this@EScheduleFragment.viewModel
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager.offscreenPageLimit = COUNT - 1
        tabs.setupWithViewPager(viewPager)
        viewPager.adapter = EScheduleAdapter(childFragmentManager)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.refreshSource.observe(this, Observer {
            when (it.status) {
                Status.ERROR -> showSnack(getString(R.string.siecomp_error_updating_info))
                Status.LOADING, Status.SUCCESS -> {}
            }
        })

        viewModel.loadSessions()
    }

    inner class EScheduleAdapter(fm: FragmentManager): FragmentPagerAdapter(fm) {
        override fun getCount() = COUNT

        override fun getItem(position: Int): Fragment {
            return EScheduleDayFragment.newInstance(position)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return SIECOMPDays[position].formatMonthDay()
        }

    }
}