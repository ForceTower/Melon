/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentMechCalculatorBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import javax.inject.Inject

class MechanicalFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    lateinit var binding: FragmentMechCalculatorBinding
    lateinit var viewModel: MechanicalViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
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

        viewModel.mechanics.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                binding.textNoData.visibility = VISIBLE
                binding.recyclerMech.visibility = GONE
            } else {
                binding.textNoData.visibility = GONE
                binding.recyclerMech.visibility = VISIBLE
            }
            mechAdapter.submitList(it)
        })

        viewModel.result.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            if (it.mean.isNaN()) {
                (activity as? UGameActivity)?.unlockAchievement(R.string.achievement_claramente_na_disney)
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!viewModel.playedMusic) {
            viewModel.playedMusic = true
            val player = MediaPlayer.create(requireContext(), R.raw.final_countdown)
            player.setVolume(0.15f, 0.15f)
            player.start()
        }
    }
}