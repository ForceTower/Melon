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

package com.forcetower.unes.feature.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.storage.database.accessors.LocationWithGroup
import com.forcetower.unes.core.vm.ScheduleViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.FragmentScheduleBinding
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.provideViewModel
import com.forcetower.unes.feature.siecomp.SiecompActivity
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class ScheduleFragment: UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: ScheduleViewModel
    private lateinit var binding: FragmentScheduleBinding

    private val linePool = RecyclerView.RecycledViewPool()
    private val lineAdapter by lazy { ScheduleLineAdapter(linePool) }
    private val blockPool = RecyclerView.RecycledViewPool()
    private val blockAdapter by lazy { ScheduleBlockAdapter(blockPool, requireContext()) }

    init {
        linePool.setMaxRecycledViews(1, 4)
        linePool.setMaxRecycledViews(2, 5)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        getTabLayout().visibility = GONE
        getToolbarTitleText().text = getString(R.string.label_schedule)
        getAppBar().elevation = 0f
        return FragmentScheduleBinding.inflate(inflater, container, false).also {
            binding = it
            binding.btnSiecompSchedule.setOnClickListener{_ -> SiecompActivity.startActivity(requireContext())}
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            recyclerScheduleLine.apply {
                setRecycledViewPool(linePool)
                adapter = lineAdapter
                itemAnimator = DefaultItemAnimator()
            }

            recyclerScheduleBlocks.apply {
                setRecycledViewPool(blockPool)
                adapter = blockAdapter
            }
        }

        viewModel = provideViewModel(factory)
        viewModel.scheduleSrc.observe(this, Observer { populateInterface(it) })
    }

    private fun populateInterface(locations: List<LocationWithGroup>) {
        Timber.d("Locations: $locations")
        binding.empty = locations.isEmpty()
        binding.executePendingBindings()
        val sorted = locations.toMutableList()
        sorted.sort()
        lineAdapter.adaptList(sorted)
        blockAdapter.adaptList(locations)
    }
}