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

package com.forcetower.uefs.feature.schedule

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.storage.database.accessors.LocationWithGroup
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentScheduleBinding
import com.forcetower.uefs.feature.profile.ProfileViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.forcetower.uefs.feature.siecomp.SIECOMPActivity
import javax.inject.Inject

class ScheduleFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: ScheduleViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var binding: FragmentScheduleBinding

    private val linePool = RecyclerView.RecycledViewPool()
    private val lineAdapter by lazy { ScheduleLineAdapter(this, viewModel, linePool) }
    private val blockPool = RecyclerView.RecycledViewPool()
    private val blockAdapter by lazy { ScheduleBlockAdapter(blockPool, this, viewModel, requireContext()) }

    init {
        linePool.setMaxRecycledViews(1, 4)
        linePool.setMaxRecycledViews(2, 5)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        profileViewModel = provideActivityViewModel(factory)

        return FragmentScheduleBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            actions = this@ScheduleFragment.viewModel
            lifecycleOwner = this@ScheduleFragment
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            recyclerScheduleLine.apply {
                setRecycledViewPool(linePool)
                adapter = lineAdapter
                itemAnimator?.run {
                    addDuration = 120L
                    moveDuration = 120L
                    changeDuration = 120L
                    removeDuration = 100L
                }
            }

            recyclerScheduleBlocks.apply {
                setRecycledViewPool(blockPool)
                adapter = blockAdapter
                itemAnimator?.run {
                    addDuration = 120L
                    moveDuration = 120L
                    changeDuration = 120L
                    removeDuration = 100L
                }
            }

            if (VersionUtils.isMarshmallow()) {
                layoutData.setOnScrollChangeListener { _, _, _, _, _ ->
                    if (layoutData.scrollY > 0)
                        appBar.elevation = getPixelsFromDp(requireContext(), 4)
                    else
                        appBar.elevation = 0f
                }
            }
        }

        binding.btnSiecompSchedule.setOnClickListener {
            val intent = Intent(requireContext(), SIECOMPActivity::class.java)
            startActivity(intent)
        }

        viewModel.scheduleSrc.observe(this, Observer { populateInterface(it) })
        profileViewModel.getMeProfile().observe(this, Observer {
            val courseId = it?.course ?: 1L
            binding.btnSiecompSchedule.visibility = if (courseId == 1L) {
                VISIBLE
            } else {
                GONE
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.appBar.elevation = 0f
    }

    private fun populateInterface(locations: List<LocationWithGroup>) {
        binding.empty = locations.isEmpty()
        binding.executePendingBindings()
        val sorted = locations.toMutableList()
        sorted.sort()
        lineAdapter.adaptList(sorted)
        blockAdapter.adaptList(locations)
    }
}