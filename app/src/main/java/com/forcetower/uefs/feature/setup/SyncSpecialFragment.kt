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

package com.forcetower.uefs.feature.setup

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.forcetower.core.injection.Injectable
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentSetupSpecialConfigBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.judemanutd.autostarter.AutoStartPermissionHelper
import java.util.Locale
import javax.inject.Inject

class SyncSpecialFragment : UFragment(), Injectable {
    @Inject
    lateinit var analytics: FirebaseAnalytics

    private lateinit var binding: FragmentSetupSpecialConfigBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentSetupSpecialConfigBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_special_to_home)
            requireActivity().finishAfterTransition()
        }

        val manufacturer = Build.MANUFACTURER.toLowerCase(Locale.getDefault())

        val bundle = bundleOf("manufacturer" to manufacturer)
        if (savedInstanceState == null) {
            analytics.logEvent("special_settings", bundle)
        }

        binding.btnConfig.setOnClickListener {
            analytics.logEvent("open_special_settings", bundle)
            val success = AutoStartPermissionHelper.getInstance().getAutoStartPermission(requireContext())
            if (success) {
                analytics.logEvent("open_special_settings_completed", bundle)
            } else {
                analytics.logEvent("open_special_settings_failed", bundle)
            }
        }
    }
}