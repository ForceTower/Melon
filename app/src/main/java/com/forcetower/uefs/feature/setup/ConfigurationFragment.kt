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

package com.forcetower.uefs.feature.setup

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.SyncFrequency
import com.forcetower.uefs.databinding.FragmentSetupConfigurationBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.google.firebase.auth.FirebaseAuth
import com.judemanutd.autostarter.AutoStartPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ConfigurationFragment : UFragment() {
    @Inject lateinit var firebaseAuth: FirebaseAuth

    private lateinit var binding: FragmentSetupConfigurationBinding
    private val viewModel: SetupViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentSetupConfigurationBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textSetupSync.setText(viewModel.getSelectedFrequency().name)
        binding.textSetupSync.setOnClickListener {
            val dialog = SelectSyncDialog()
            dialog.setCallback(
                object : FrequencySelectionCallback {
                    override fun onSelected(frequency: SyncFrequency) {
                        viewModel.setSelectedFrequency(frequency)
                        binding.textSetupSync.setText(frequency.name)
                    }
                }
            )
            dialog.show(childFragmentManager, "dialog_sync")
        }

        binding.btnNext.setOnClickListener {
            completeSetup()
            decideNext()
        }
    }

    private fun decideNext() {
        val autoStart = AutoStartPermissionHelper.getInstance().isAutoStartPermissionAvailable(requireContext())
        val brands = when (Build.BRAND.toLowerCase(Locale.getDefault())) {
            "samsung" -> true
            else -> false
        }
        if (autoStart || brands) {
            findNavController().navigate(R.id.action_configuration_to_special)
        } else {
            findNavController().navigate(R.id.action_configuration_to_home)
            requireActivity().finishAfterTransition()
        }
    }

    private fun completeSetup() {
        viewModel.setFrequencyAndComplete(viewModel.getSelectedFrequency())
    }
}
