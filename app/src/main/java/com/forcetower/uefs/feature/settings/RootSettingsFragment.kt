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

package com.forcetower.uefs.feature.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import androidx.preference.PreferenceFragmentCompat
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import javax.inject.Inject

class RootSettingsFragment : PreferenceFragmentCompat(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    lateinit var viewModel: SettingsViewModel

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        onPreferenceChange(shared, key)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_start, rootKey)
        viewModel = provideActivityViewModel(factory)
        toggleDarkModeVisibility(false)
        viewModel.isDarkModeEnabled.observe(this, Observer { toggleDarkModeVisibility(it) })
    }

    private fun toggleDarkModeVisibility(visible: Boolean?) {
        val isVisible = visible ?: false
        val pNightMode = findPreference("stg_night_mode")
        if (pNightMode != null) {
            pNightMode.isVisible = isVisible
        }
    }

    private fun onPreferenceChange(preference: SharedPreferences, key: String) {
        when (key) {
            "stg_night_mode" -> onNightModeChanged(preference.getString(key, "0")?.toIntOrNull() ?: 0)
        }
    }

    private fun onNightModeChanged(mode: Int) {
        when (mode) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        setupPreferenceSummary()
        getSharedPreferences().edit().putBoolean("will_recreate_home", true).apply()
        activity?.recreate()
    }

    private fun setupPreferenceSummary() {
        val pNightMode = findPreference("stg_night_mode")
        if (pNightMode != null) {
            val current = getSharedPreferences().getString("stg_night_mode", "0")
            val resource = when (current) {
                "0" -> R.string.night_mode_follow_system
                "1" -> R.string.night_mode_always_light
                "2" -> R.string.night_mode_always_dark
                else -> R.string.night_mode_follow_system
            }
            pNightMode.setSummary(resource)
        }
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences().registerOnSharedPreferenceChangeListener(listener)
        setupPreferenceSummary()
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun getSharedPreferences() = preferenceManager.sharedPreferences
}