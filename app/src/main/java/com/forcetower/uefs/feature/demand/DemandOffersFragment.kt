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

package com.forcetower.uefs.feature.demand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentDemandOffersBinding
import com.forcetower.uefs.feature.shared.NavigationFragment
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DemandOffersFragment : UFragment(), NavigationFragment {
    private val viewModel: DemandViewModel by activityViewModels()
    private lateinit var binding: FragmentDemandOffersBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDemandOffersBinding.inflate(inflater, container, false).apply {
            viewModel = this@DemandOffersFragment.viewModel
            lifecycleOwner = this@DemandOffersFragment
            incToolbar.textToolbarTitle.text = getString(R.string.label_demand_title)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val offersAdapter = DemandOffersAdapter(this, viewModel)
        binding.offersRecycler.apply {
            adapter = offersAdapter
            itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        binding.incToolbar.appBar.elevation = if (recyclerView.canScrollVertically(-1)) getPixelsFromDp(requireContext(), 6) else 0f
                    }
                }
            )
        }
        viewModel.offers.observe(
            viewLifecycleOwner,
            Observer {
                val data = it.data
                if (data != null) {
                    offersAdapter.currentList = data
                }
            }
        )
    }
}
