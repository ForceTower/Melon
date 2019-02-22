/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.feature.siecomp.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentEventScheduleBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.forcetower.uefs.feature.siecomp.SIECOMPEventViewModel
import com.forcetower.uefs.feature.siecomp.session.EventSessionDetailsActivity
import com.google.android.material.tabs.TabLayout
import javax.inject.Inject

class ScheduleFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: SIECOMPEventViewModel
    private lateinit var binding: FragmentEventScheduleBinding
    private lateinit var viewPager: ViewPager
    private lateinit var tabs: TabLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        FragmentEventScheduleBinding.inflate(inflater, container, false).also {
            binding = it
            tabs = binding.tabLayout
            viewPager = binding.pagerSchedule
        }.apply {
            lifecycleOwner = this@ScheduleFragment
            viewModel = this@ScheduleFragment.viewModel
            executePendingBindings()
        }

        viewModel.navigateToSessionAction.observe(this, EventObserver {
            openSessionDetails(it)
        })

        viewModel.snackbarMessenger.observe(this, EventObserver {
            showSnack(getString(it))
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager.offscreenPageLimit = COUNT - 1
        tabs.setupWithViewPager(viewPager)
        viewPager.adapter = ScheduleAdapter(childFragmentManager)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.refreshSource.observe(this, Observer {
            when (it.status) {
                Status.ERROR -> showSnack(getString(R.string.siecomp_error_updating_info))
                Status.LOADING, Status.SUCCESS -> {}
            }
        })

        if (!viewModel.sessionsLoaded) {
            viewModel.sessionsLoaded = true
            viewModel.loadSessions()
        }
    }

    private fun openSessionDetails(id: Long) {
        startActivity(EventSessionDetailsActivity.startIntent(requireContext(), id))
    }

    inner class ScheduleAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getCount() = COUNT

        override fun getItem(position: Int): Fragment {
            return ScheduleDayFragment.newInstance(position)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return TimeUtils.EventDays[position].formatMonthDay()
        }
    }

    companion object {
        private val COUNT = TimeUtils.EventDays.size
    }
}