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

package com.forcetower.uefs.feature.demand.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.databinding.ObservableFloat
import androidx.fragment.app.activityViewModels
import com.forcetower.uefs.databinding.FragmentDemandOverviewBinding
import com.forcetower.uefs.feature.demand.DemandViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.widget.BottomSheetBehavior
import com.forcetower.uefs.widget.BottomSheetBehavior.Companion.STATE_COLLAPSED
import com.forcetower.uefs.widget.BottomSheetBehavior.Companion.STATE_EXPANDED
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DemandOverviewFragment : UFragment() {
    companion object {
        private const val ALPHA_CHANGEOVER = 0.33f
        private const val ALPHA_HEADER_MAX = 0.67f
    }

    private val viewModel: DemandViewModel by activityViewModels()
    private lateinit var binding: FragmentDemandOverviewBinding
    private lateinit var behavior: BottomSheetBehavior<*>

    private var headerAlpha = ObservableFloat(1f)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDemandOverviewBinding.inflate(inflater, container, false).apply {
            viewModel = this@DemandOverviewFragment.viewModel
            headerAlpha = this@DemandOverviewFragment.headerAlpha
            lifecycleOwner = this@DemandOverviewFragment
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        behavior = BottomSheetBehavior.from(binding.demandOverviewSheet)

        val offersAdapter = OffersOverviewAdapter(this, viewModel)
        binding.selectedRecycler.apply {
            adapter = offersAdapter
            setHasFixedSize(true)
        }

        viewModel.selected.observe(
            viewLifecycleOwner,
            {
                if (it != null) offersAdapter.submitList(it)
            }
        )

        behavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    updateFilterHeadersAlpha(slideOffset)
                }
            }
        )

        binding.collapseArrow.setOnClickListener {
            behavior.state = STATE_COLLAPSED
        }

        binding.expandArrow.setOnClickListener {
            behavior.state = STATE_EXPANDED
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
