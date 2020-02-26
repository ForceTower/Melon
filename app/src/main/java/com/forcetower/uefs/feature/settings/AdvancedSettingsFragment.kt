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

package com.forcetower.uefs.feature.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.forcetower.core.injection.Injectable
import com.forcetower.uefs.R
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.core.work.sync.SyncMainWorker
import com.forcetower.uefs.feature.messages.MessagesDFMViewModel
import com.forcetower.uefs.feature.web.CustomTabActivityHelper
import com.google.android.material.snackbar.Snackbar
import com.judemanutd.autostarter.AutoStartPermissionHelper
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class AdvancedSettingsFragment : PreferenceFragmentCompat(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private val viewModel: SettingsViewModel by activityViewModels { factory }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        onPreferenceChange(shared, key)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_advanced, rootKey)
        updateDozePreferences()

        if (!getSharedPreferences().isStudentFromUEFS()) {
            findPreference<SwitchPreference>("stg_advanced_aeri_tab")?.isVisible = false
        }

        findPreference<Preference>("stg_advanced_auto_start")?.let {
            it.setOnPreferenceClickListener {
                val result = AutoStartPermissionHelper.getInstance().getAutoStartPermission(requireContext())
                if (!result) {
                    Snackbar.make(requireView(), getString(R.string.settings_auto_start_manager_not_found), Snackbar.LENGTH_SHORT).show()
                }
                true
            }
        }

        findPreference<Preference>("stg_advanced_battery_optimization")?.let {
            it.setOnPreferenceClickListener {
                CustomTabActivityHelper.openCustomTab(
                    requireActivity(),
                    CustomTabsIntent.Builder()
                        .setToolbarColor(ContextCompat.getColor(requireContext(), R.color.blue_accent))
                        .addDefaultShareMenuItem()
                        .build(),
                    Uri.parse("https://dontkillmyapp.com/${Build.BRAND.toLowerCase(Locale.getDefault())}"))
                true
            }
        }
    }

    private fun updateDozePreferences() {
        if (VersionUtils.isMarshmallow()) {
            findPreference<SwitchPreference>("stg_advanced_ignore_doze")?.let {
                val pm = context?.getSystemService(Context.POWER_SERVICE) as PowerManager?
                val ignoring = pm?.isIgnoringBatteryOptimizations(requireContext().packageName) ?: false
                it.isChecked = ignoring
            }
        } else {
            findPreference<SwitchPreference>("stg_advanced_ignore_doze")?.let {
                it.isEnabled = false
                it.setSummary(R.string.settings_adv_doze_mode_info_disabled)
            }
        }
    }

    private fun onPreferenceChange(preference: SharedPreferences, key: String) {
        when (key) {
            "stg_advanced_aeri_tab" -> aeriTabs(preference.getBoolean(key, true))
            "stg_advanced_ignore_doze" -> ignoreDoze(preference.getBoolean(key, false))
        }
    }

    private fun aeriTabs(enabled: Boolean) {
        if (!enabled) {
            viewModel.uninstallModuleIfExists(MessagesDFMViewModel.AERI_MODULE)
        }
    }

    @SuppressLint("BatteryLife")
    private fun ignoreDoze(ignore: Boolean) {
        if (!VersionUtils.isMarshmallow()) return
        if (!ignore) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        } else {
            val pm = context?.getSystemService(Context.POWER_SERVICE) as PowerManager?
            val ignoring = pm?.isIgnoringBatteryOptimizations(requireContext().packageName) ?: false
            Timber.d("Ignoring? $ignoring")
            if (!ignoring) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context?.packageName}")
                }
                startActivityForResult(intent, REQUEST_IGNORE_DOZE)
            }
        }
    }

    private fun setDozePreference(enabled: Boolean) {
        findPreference<SwitchPreference>("stg_advanced_ignore_doze")?.let {
            it.isChecked = enabled
        }
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences().registerOnSharedPreferenceChangeListener(listener)
        updateDozePreferences()
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun getSharedPreferences() = preferenceManager.sharedPreferences

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IGNORE_DOZE -> {
                if (resultCode == Activity.RESULT_OK) {
                    SyncMainWorker.stopWorker(requireContext())
                    SyncMainWorker.createWorker(requireContext(), 60)
                    Snackbar.make(requireView(), getString(R.string.settings_accept_ignore_doze), Snackbar.LENGTH_LONG).show()
                    getSharedPreferences().edit().putString("stg_sync_frequency", "60").apply()
                    setDozePreference(true)
                } else {
                    Snackbar.make(requireView(), getString(R.string.settings_refuse_ignore_doze), Snackbar.LENGTH_LONG).show()
                    getSharedPreferences().edit().putBoolean("stg_advanced_ignore_doze", false).apply()
                    setDozePreference(false)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_IGNORE_DOZE = 40000
    }
}