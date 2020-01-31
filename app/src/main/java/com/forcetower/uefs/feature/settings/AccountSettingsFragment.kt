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

package com.forcetower.uefs.feature.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import com.forcetower.uefs.R
import com.forcetower.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import javax.inject.Inject

class AccountSettingsFragment : PreferenceFragmentCompat(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    lateinit var viewModel: SettingsViewModel

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        onPreferenceChange(shared, key)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_account, rootKey)
        viewModel = provideActivityViewModel(factory)
    }

    private fun onPreferenceChange(preference: SharedPreferences, key: String) {
        when (key) {
            "stg_choice_score" -> toggleScoreChoice(preference.getBoolean(key, false))
        }
    }

    private fun toggleScoreChoice(calculated: Boolean) {
        if (calculated) {
            viewModel.getAllTheGrades()
            Toast.makeText(requireContext(), R.string.all_the_grades_will_be_downloaded, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences().registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun getSharedPreferences() = preferenceManager.sharedPreferences
}