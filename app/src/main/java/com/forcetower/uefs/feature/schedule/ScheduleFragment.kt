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

package com.forcetower.uefs.feature.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.storage.database.accessors.LocationWithGroup
import com.forcetower.uefs.core.vm.ProfileViewModel
import com.forcetower.uefs.core.vm.ScheduleViewModel
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentScheduleBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.provideActivityViewModel
import com.forcetower.uefs.feature.shared.provideViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import timber.log.Timber
import javax.inject.Inject

class ScheduleFragment: UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    @Inject
    lateinit var firebaseStorage: FirebaseStorage

    private lateinit var viewModel: ScheduleViewModel
    private lateinit var profileViewModel: ProfileViewModel
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
        viewModel = provideViewModel(factory)
        profileViewModel = provideActivityViewModel(factory)

        return FragmentScheduleBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            profileViewModel = this@ScheduleFragment.profileViewModel
            firebaseStorage = this@ScheduleFragment.firebaseStorage
            firebaseUser = this@ScheduleFragment.firebaseAuth.currentUser
            setLifecycleOwner(this@ScheduleFragment)
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
        }

        viewModel.scheduleSrc.observe(this, Observer { populateInterface(it) })
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