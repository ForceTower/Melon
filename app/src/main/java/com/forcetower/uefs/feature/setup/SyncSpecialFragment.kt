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

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.forcetower.core.utils.ViewUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.databinding.FragmentSetupSpecialConfigBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.web.CustomTabActivityHelper
import com.google.firebase.analytics.FirebaseAnalytics
import com.judemanutd.autostarter.AutoStartPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SyncSpecialFragment : UFragment() {
    @Inject lateinit var analytics: FirebaseAnalytics

    private lateinit var binding: FragmentSetupSpecialConfigBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentSetupSpecialConfigBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    @SuppressLint("BatteryLife")
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

        binding.labelAutoStartPath.setOnClickListener {
            CustomTabActivityHelper.openCustomTab(
                requireActivity(),
                CustomTabsIntent.Builder()
                    .setDefaultColorSchemeParams(
                        CustomTabColorSchemeParams
                            .Builder()
                            .setToolbarColor(ViewUtils.attributeColorUtils(requireContext(), R.attr.colorPrimary))
                            .build()
                    )
                    .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                    .build(),
                Uri.parse("https://dontkillmyapp.com/${Build.BRAND.toLowerCase(Locale.getDefault())}")
            )
        }
        if (VersionUtils.isMarshmallow()) {
            binding.btnDoze.setOnClickListener {
                if (Build.BRAND.equals("xiaomi", ignoreCase = true) || Build.BRAND.equals("redmi", ignoreCase = true)) {
                    try {
                        val intent = Intent()
                        intent.component = ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
                        intent.putExtra("package_name", requireContext().packageName)
                        intent.putExtra("package_label", getText(R.string.app_name))
                        startActivity(intent)
                    } catch (throwable: ActivityNotFoundException) {
                        Timber.e(throwable, "Xiaomi without a power keeper")
                        showSnack(getString(R.string.settings_ignore_doze_failed))
                    }
                } else {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                    }
                    try {
                        startActivity(intent)
                    } catch (error: ActivityNotFoundException) {
                        Timber.e(error, "This device doesn't support ignore optimizations")
                        showSnack(getString(R.string.settings_ignore_doze_failed))
                    }
                }
            }
        } else {
            binding.btnDoze.visibility = View.GONE
        }
    }
}
