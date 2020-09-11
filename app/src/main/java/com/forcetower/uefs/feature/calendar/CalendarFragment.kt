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

package com.forcetower.uefs.feature.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentCalendarBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CalendarFragment : UFragment() {
    private val viewModel: AcademicCalendarViewModel by viewModels()
    private lateinit var binding: FragmentCalendarBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentCalendarBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            incToolbar.textToolbarTitle.text = getString(R.string.label_calendar)
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = CalendarAdapter()
        binding.recyclerAcademicCalendar.adapter = adapter
        binding.recyclerAcademicCalendar.itemAnimator?.apply {
            addDuration = 160L
            moveDuration = 160L
            changeDuration = 160L
            removeDuration = 120L
        }

        viewModel.calendar.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })
    }
}
