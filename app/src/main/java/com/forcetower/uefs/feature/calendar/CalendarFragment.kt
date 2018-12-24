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

package com.forcetower.uefs.feature.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentCalendarBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import javax.inject.Inject

class CalendarFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: AcademicCalendarViewModel
    private lateinit var binding: FragmentCalendarBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
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

        viewModel.calendar.observe(this, Observer { adapter.submitList(it) })
    }
}