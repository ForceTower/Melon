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

package com.forcetower.uefs.feature.mechcalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentMechCalculatorBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.UGameActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MechanicalFragment : UFragment() {
    private val viewModel: MechanicalViewModel by activityViewModels()
    private lateinit var binding: FragmentMechCalculatorBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentMechCalculatorBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.incToolbar.textToolbarTitle.text = getString(R.string.label_mech_calculator)
        val mechAdapter = MechanicsAdapter(this, viewModel)
        binding.recyclerMech.run {
            adapter = mechAdapter
        }

        binding.fabCreateValue.setOnClickListener {
            val dialog = MechCreateDialog()
            dialog.show(childFragmentManager, "mech_create_dialog")
        }

        binding.run {
            result = viewModel.result
            interactor = viewModel
            lifecycleOwner = this@MechanicalFragment
            executePendingBindings()
        }

        viewModel.mechanics.observe(
            viewLifecycleOwner,
            Observer {
                if (it.isEmpty()) {
                    binding.textNoData.visibility = VISIBLE
                    binding.recyclerMech.visibility = GONE
                } else {
                    binding.textNoData.visibility = GONE
                    binding.recyclerMech.visibility = VISIBLE
                }
                mechAdapter.submitList(it)
            }
        )

        viewModel.result.observe(
            viewLifecycleOwner,
            Observer {
                it ?: return@Observer
                if (it.mean.isNaN()) {
                    (activity as? UGameActivity)?.unlockAchievement(R.string.achievement_claramente_na_disney)
                }
            }
        )
    }
}
