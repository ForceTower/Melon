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

package com.forcetower.uefs.feature.setup

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentSetupConfigurationBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class ConfigurationFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private lateinit var binding: FragmentSetupConfigurationBinding
    private lateinit var viewModel: SetupViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        return FragmentSetupConfigurationBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textSetupSync.setText(viewModel.getSelectedFrequency().name)
        binding.textSetupSync.setOnClickListener {
            val dialog = SelectSyncDialog()
            dialog.setCallback(object : FrequencySelectionCallback {
                override fun onSelected(frequency: Frequency) {
                    viewModel.setSelectedFrequency(frequency)
                    binding.textSetupSync.setText(frequency.name)
                }
            })
            dialog.show(childFragmentManager, "dialog_sync")
        }

        binding.btnNext.setOnClickListener {
            completeSetup()
            decideNext()
        }
    }

    private fun decideNext() {
        val manufacturer = Build.MANUFACTURER.toLowerCase()

        when (manufacturer) {
            "xiaomi" -> findNavController().navigate(R.id.action_configuration_to_special)
            "oppo" -> findNavController().navigate(R.id.action_configuration_to_special)
            "vivo" -> findNavController().navigate(R.id.action_configuration_to_special)
            "lenovo" -> findNavController().navigate(R.id.action_configuration_to_special)
            "honor" -> findNavController().navigate(R.id.action_configuration_to_special)
            "huawei" -> findNavController().navigate(R.id.action_configuration_to_special)
            else -> {
                findNavController().navigate(R.id.action_configuration_to_home)
                requireActivity().finishAfterTransition()
            }
        }
    }

    private fun completeSetup() {
        viewModel.setFrequencyAndComplete(viewModel.getSelectedFrequency())
    }
}