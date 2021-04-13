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

package com.forcetower.uefs.feature.siecomp.schedule

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.viewpager.widget.ViewPager
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import com.forcetower.uefs.databinding.FragmentEventScheduleBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.siecomp.SIECOMPEventViewModel
import com.forcetower.uefs.feature.siecomp.editor.SIECOMPEditorActivity
import com.forcetower.uefs.feature.siecomp.session.EventSessionDetailsActivity
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScheduleFragment : UFragment() {
    private val viewModel: SIECOMPEventViewModel by activityViewModels()
    private lateinit var binding: FragmentEventScheduleBinding
    private lateinit var viewPager: ViewPager
    private lateinit var tabs: TabLayout
    private var count = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        FragmentEventScheduleBinding.inflate(inflater, container, false).also {
            binding = it
            tabs = binding.tabLayout
            viewPager = binding.pagerSchedule
        }.apply {
            lifecycleOwner = this@ScheduleFragment
            viewModel = this@ScheduleFragment.viewModel
            executePendingBindings()
        }

        viewModel.navigateToSessionAction.observe(
            viewLifecycleOwner,
            EventObserver {
                openSessionDetails(it)
            }
        )

        viewModel.snackbarMessenger.observe(
            viewLifecycleOwner,
            EventObserver {
                showSnack(getString(it))
            }
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager.offscreenPageLimit = COUNT - 1
        tabs.setupWithViewPager(viewPager)
        viewPager.adapter = ScheduleAdapter(childFragmentManager)
        binding.textToolbarTitle.setOnClickListener {
            if (++count == 10) {
                count = 0
                startActivity(Intent(requireContext(), SIECOMPEditorActivity::class.java))
            }
        }
        viewModel.access.observe(
            viewLifecycleOwner,
            {
                binding.createSessionFloat.visibility = if (it != null) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        )

        viewModel.refreshSource.observe(
            viewLifecycleOwner,
            {
                when (it.status) {
                    Status.ERROR -> showSnack(getString(R.string.siecomp_error_updating_info))
                    Status.LOADING, Status.SUCCESS -> {}
                }
            }
        )

        if (!viewModel.sessionsLoaded) {
            viewModel.sessionsLoaded = true
            viewModel.loadSessions()
        }
    }

    private fun openSessionDetails(id: Long) {
        startActivity(EventSessionDetailsActivity.startIntent(requireContext(), id))
    }

    inner class ScheduleAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount() = COUNT

        override fun getItem(position: Int): Fragment {
            return ScheduleDayFragment.newInstance(position)
        }

        override fun getPageTitle(position: Int): CharSequence {
            return TimeUtils.EventDays[position].formatMonthDay()
        }
    }

    companion object {
        private val COUNT = TimeUtils.EventDays.size
    }
}
