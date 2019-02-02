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

        viewModel.mechanics.observe(this, Observer {
            if (it.isEmpty()) {
                binding.textNoData.visibility = VISIBLE
                binding.recyclerMech.visibility = GONE
            } else {
                binding.textNoData.visibility = GONE
                binding.recyclerMech.visibility = VISIBLE
            }
            mechAdapter.submitList(it)
        })

        viewModel.result.observe(this, Observer {
            it ?: return@Observer
            if (it.mean == Double.NaN) {
                (activity as? UGameActivity)?.unlockAchievement(R.string.achievement_claramente_na_disney)
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!viewModel.playedMusic) {
            viewModel.playedMusic
            val player = MediaPlayer.create(requireContext(), R.raw.final_countdown)
            player.setVolume(0.1f, 0.1f)
            player.start()
        }
    }
}