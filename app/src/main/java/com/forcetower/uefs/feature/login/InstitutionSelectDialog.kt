package com.forcetower.uefs.feature.login

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.core.constants.Constants.SELECTED_INSTITUTION_KEY
import com.forcetower.uefs.databinding.DialogInstitutionSelectorBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import timber.log.Timber

class InstitutionSelectDialog : RoundedDialog() {
    private lateinit var binding: DialogInstitutionSelectorBinding

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
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