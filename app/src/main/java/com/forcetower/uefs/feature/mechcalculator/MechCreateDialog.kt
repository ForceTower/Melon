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

package com.forcetower.uefs.feature.mechcalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.DialogCreateMechValueBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MechCreateDialog : RoundedDialog() {
    private val viewModel: MechanicalViewModel by activityViewModels()
    private lateinit var binding: DialogCreateMechValueBinding

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogCreateMechValueBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textInputGrade.run {
            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && isEnabled) {
                    val string = text.toString()
                    if (string.isNotBlank()) {
                        val parsed = string.replace(",", ".").toDoubleOrNull()
                        error = if (parsed == null) {
                            context.getString(R.string.field_must_be_a_number)
                        } else {
                            null
                        }
                    }
                }
            }
        }

        binding.textInputWeight.run {
            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val string = text.toString()
                    if (string.isNotBlank()) {
                        val parsed = string.replace(",", ".").toDoubleOrNull()
                        error = if (parsed == null) {
                            context.getString(R.string.field_must_be_a_number)
                        } else {
                            null
                        }
                    }
                }
            }
        }

        binding.checkboxWildcard.setOnCheckedChangeListener { _, isChecked ->
            binding.textInputGrade.isEnabled = !isChecked
            binding.textInputGrade.text?.clear()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnOk.setOnClickListener {
            val checked = binding.checkboxWildcard.isChecked
            val grade = binding.textInputGrade.text.toString().replace(",", ".").toDoubleOrNull()
            val weight = binding.textInputWeight.text.toString().replace(",", ".").toDoubleOrNull()
            var error = false

            if (!checked && grade == null) {
                binding.textInputGrade.error = requireContext().getString(R.string.field_must_be_a_number)
                error = true
            }

            if (weight == null) {
                binding.textInputWeight.error = requireContext().getString(R.string.field_must_be_a_number)
                error = true
            }

            if (!error && weight != null) {
                viewModel.onAddValue(MechValue(weight, if (checked) null else grade))
                dismiss()
            } else {
                Timber.d("error: $error, weight $weight")
            }
        }
    }
}
