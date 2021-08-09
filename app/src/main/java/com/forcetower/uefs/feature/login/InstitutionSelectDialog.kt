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

package com.forcetower.uefs.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.core.constants.Constants.SELECTED_INSTITUTION_KEY
import com.forcetower.uefs.databinding.DialogInstitutionSelectorBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import timber.log.Timber

class InstitutionSelectDialog : RoundedDialog() {
    private lateinit var binding: DialogInstitutionSelectorBinding

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return DialogInstitutionSelectorBinding.inflate(inflater, container, false).also {
            binding = it
            it.btnOk.setOnClickListener {
                saveSelectedInstitution()
                dismiss()
            }
            it.btnCancel.setOnClickListener {
                dismiss()
            }
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateInstitutions()
    }

    private fun populateInstitutions() {
        val institutions = SagresNavigator.getSupportedInstitutions()
        binding.pickerInstitution.minValue = 1
        binding.pickerInstitution.maxValue = institutions.size
        binding.pickerInstitution.displayedValues = institutions
    }

    private fun saveSelectedInstitution() {
        val institutions = SagresNavigator.getSupportedInstitutions()
        val selected = institutions[binding.pickerInstitution.value - 1]
        Timber.d("Selected $selected")
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString(SELECTED_INSTITUTION_KEY, selected).apply()
        SagresNavigator.instance.setSelectedInstitution(selected)
    }
}
