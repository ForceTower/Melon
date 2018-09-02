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

package com.forcetower.unes.feature.siecomp.day

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.storage.database.accessors.SessionWithData
import com.forcetower.unes.core.storage.resource.Resource
import com.forcetower.unes.core.storage.resource.Status
import com.forcetower.unes.core.vm.EventViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.FragmentSiecompScheduleDayBinding
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.clearDecorations
import com.forcetower.unes.feature.shared.provideActivityViewModel
import com.forcetower.unes.feature.siecomp.ETimeUtils
import com.forcetower.unes.feature.siecomp.ETimeUtils.SIECOMP_TIMEZONE
import timber.log.Timber
import javax.inject.Inject

class EScheduleDayFragment: UFragment(), Injectable {

    companion object {
        private const val ARG_EVENT_DAY = "arg.EVENT_DAY"

        fun newInstance(day: Int): EScheduleDayFragment {
            val args = bundleOf(ARG_EVENT_DAY to day)
            return EScheduleDayFragment().apply { arguments = args }
        }
    }

    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: EventViewModel
    private lateinit var binding: FragmentSiecompScheduleDayBinding

    private val eventDay: Int by lazy {
        val args = arguments?: throw IllegalStateException("No Arguments")
        args.getInt(ARG_EVENT_DAY)
    }

    private lateinit var adapter: EScheduleDayAdapter
    private val tagViewPool = RecyclerView.RecycledViewPool()
    private val sessionViewPool = RecyclerView.RecycledViewPool()

    init {
        tagViewPool.setMaxRecycledViews(0, 15)
        sessionViewPool.setMaxRecycledViews(0, 10)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        binding = FragmentSiecompScheduleDayBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@EScheduleDayFragment)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = EScheduleDayAdapter(tagViewPool, ETimeUtils.SIECOMP_TIMEZONE)
        binding.recyclerDaySchedule.apply {
            setRecycledViewPool(sessionViewPool)
            adapter = this@EScheduleDayFragment.adapter
            (layoutManager as LinearLayoutManager).recycleChildrenOnDetach = true
            (itemAnimator as DefaultItemAnimator).run {
                supportsChangeAnimations = false
                addDuration = 160L
                moveDuration = 160L
                changeDuration = 160L
                removeDuration = 120L
            }
        }

        //TODO Maybe move to current event
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.getSessionsFromDayLocal(eventDay).observe(this, Observer {
            it?: return@Observer
            populateInterface(it)
        })
    }

    private fun populateInterface(data: List<SessionWithData>) {
        adapter.submitList(data)
        binding.recyclerDaySchedule.run {
            doOnNextLayout {
                clearDecorations()
                if (data.isNotEmpty()) {
                    addItemDecoration(
                        ScheduleItemHeaderDecoration(
                            it.context, data, SIECOMP_TIMEZONE
                        )
                    )
                }
            }
        }
    }

}