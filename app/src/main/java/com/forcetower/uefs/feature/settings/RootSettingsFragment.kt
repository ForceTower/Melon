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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.R
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class RootSettingsFragment : PreferenceFragmentCompat() {
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        onPreferenceChange(shared, key)
    }

    private val viewModel by viewModels<SettingsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findPreference<Preference>("settings_device_id")?.setOnPreferenceClickListener {
            copyIdToClipboard()
            true
        }
        viewModel.deviceId.observe(viewLifecycleOwner) {
            findPreference<Preference>("settings_device_id")?.summary = it
        }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_start, rootKey)
    }

    private fun onPreferenceChange(preference: SharedPreferences, key: String?) {
        when (key) {
            "stg_night_mode" -> changeDarkThemePrefs(preference.getString(key, "-1")?.toIntOrNull() ?: -1)
            else -> Timber.d("Else... $key")
        }
    }

    private fun copyIdToClipboard() {
        val clipboard: ClipboardManager? = requireContext().getSystemService()
        clipboard ?: return
        val content = viewModel.deviceId.value ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("unes-device-id", content))
        Toast.makeText(requireContext(), R.string.device_id_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun changeDarkThemePrefs(@AppCompatDelegate.NightMode mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences()?.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences()?.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun getSharedPreferences() = preferenceManager.sharedPreferences
}
