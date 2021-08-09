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

package com.forcetower.uefs.feature.siecomp.onboarding

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.viewModels
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import com.forcetower.uefs.databinding.FragmentEventOnboardingBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.ViewPagerPager
import com.forcetower.uefs.feature.siecomp.schedule.EventScheduleActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingFragment : UFragment() {
    @Inject lateinit var preferences: SharedPreferences
    private val viewModel: OnboardingViewModel by viewModels()

    private lateinit var binding: FragmentEventOnboardingBinding
    private lateinit var pagerPager: ViewPagerPager
    private val handler = Handler(Looper.getMainLooper())

    private val advancePager: Runnable = object : Runnable {
        override fun run() {
            pagerPager.advance()
            handler.postDelayed(this, AUTO_ADVANCE_DELAY)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

        viewModel.navigateToEventActivity.observe(
            viewLifecycleOwner,
            EventObserver {
                requireActivity().run {
                    preferences.edit().putBoolean("siecomp_xxii_onboarding_completed_2", true).apply()
                    startActivity(Intent(this, EventScheduleActivity::class.java))
                    finish()
                }
            }
        )
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
