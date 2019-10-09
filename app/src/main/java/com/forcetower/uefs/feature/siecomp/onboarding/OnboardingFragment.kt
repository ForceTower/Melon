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

package com.forcetower.uefs.feature.siecomp.onboarding

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentEventOnboardingBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.ViewPagerPager
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.forcetower.uefs.feature.siecomp.schedule.EventScheduleActivity
import javax.inject.Inject

class OnboardingFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var preferences: SharedPreferences
    private lateinit var viewModel: OnboardingViewModel
    private lateinit var binding: FragmentEventOnboardingBinding
    private lateinit var pagerPager: ViewPagerPager
    private val handler = Handler()

    private val advancePager: Runnable = object : Runnable {
        override fun run() {
            pagerPager.advance()
            handler.postDelayed(this, AUTO_ADVANCE_DELAY)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        binding = FragmentEventOnboardingBinding.inflate(inflater, container, false).apply {
            viewModel = this@OnboardingFragment.viewModel
            lifecycleOwner = this@OnboardingFragment
            pager.adapter = OnboardingAdapter(childFragmentManager)
            pagerPager = ViewPagerPager(pager)

            pager.setOnTouchListener { _, _ ->
                handler.removeCallbacks(advancePager)
                false
            }
        }

        viewModel.navigateToEventActivity.observe(this, EventObserver {
            requireActivity().run {
                preferences.edit().putBoolean("siecomp_xxii_onboarding_completed_2", true).apply()
                startActivity(Intent(this, EventScheduleActivity::class.java))
                finish()
            }
        })
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        handler.postDelayed(advancePager, AUTO_ADVANCE_DELAY)
    }

    override fun onDetach() {
        handler.removeCallbacks(advancePager)
        super.onDetach()
    }

    inner class OnboardingAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val fragments = if (TimeUtils.eventHasStarted()) {
            arrayOf(
                WelcomeFragment(),
                CustomizeScheduleFragment()
            )
        } else {
            arrayOf(
                WelcomeFragment(),
                CountdownFragment(),
                CustomizeScheduleFragment()
            )
        }

        override fun getItem(position: Int) = fragments[position]
        override fun getCount() = fragments.size
    }

    companion object {
        private const val AUTO_ADVANCE_DELAY = 3_000L
    }
}