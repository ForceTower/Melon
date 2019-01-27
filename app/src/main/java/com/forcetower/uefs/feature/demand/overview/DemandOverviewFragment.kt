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

package com.forcetower.uefs.feature.demand.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnLayout
import androidx.databinding.ObservableFloat
import androidx.lifecycle.Observer
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentDemandOverviewBinding
import com.forcetower.uefs.feature.demand.DemandViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.forcetower.uefs.widget.BottomSheetBehavior
import com.forcetower.uefs.widget.BottomSheetBehavior.Companion.STATE_COLLAPSED
import com.forcetower.uefs.widget.BottomSheetBehavior.Companion.STATE_EXPANDED
import javax.inject.Inject

class DemandOverviewFragment : UFragment(), Injectable {
    companion object {
        private const val ALPHA_CHANGEOVER = 0.33f
        private const val ALPHA_DESC_MAX = 0f
        private const val ALPHA_HEADER_MAX = 0.67f
    }

    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: DemandViewModel
    private lateinit var binding: FragmentDemandOverviewBinding
    private lateinit var behavior: BottomSheetBehavior<*>

    private var headerAlpha = ObservableFloat(1f)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        binding = FragmentDemandOverviewBinding.inflate(inflater, container, false).apply {
            viewModel = this@DemandOverviewFragment.viewModel
            headerAlpha = this@DemandOverviewFragment.headerAlpha
            setLifecycleOwner(this@DemandOverviewFragment)
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        behavior = BottomSheetBehavior.from(binding.demandOverviewSheet)

        val offersAdapter = OffersOverviewAdapter(this, viewModel)
        binding.selectedRecycler.apply {
            adapter = offersAdapter
            setHasFixedSize(true)
        }

        viewModel.offers.observe(this, Observer {
            when (it.status) {
                Status.SUCCESS -> offersAdapter.submitList(it.data!!.filter { d -> d.selected })
                Status.ERROR -> Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show()
                Status.LOADING -> {
                    val data = it.data
                    if (data != null) offersAdapter.submitList(data)
                }
            }
        })

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                updateFilterHeadersAlpha(slideOffset)
            }
        })

        binding.collapseArrow.setOnClickListener {
            behavior.state = STATE_COLLAPSED
        }

        binding.demandOverviewSheet.doOnLayout {
            val slideOffset = when (behavior.state) {
                STATE_EXPANDED -> 1f
                STATE_COLLAPSED -> 0f
                else /*BottomSheetBehavior.STATE_HIDDEN*/ -> -1f
            }
            updateFilterHeadersAlpha(slideOffset)
        }
    }

    private fun updateFilterHeadersAlpha(slideOffset: Float) {
        headerAlpha.set(offsetToAlpha(slideOffset, ALPHA_CHANGEOVER, ALPHA_HEADER_MAX))
    }

    private fun offsetToAlpha(value: Float, rangeMin: Float, rangeMax: Float): Float {
        return ((value - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)
    }
}