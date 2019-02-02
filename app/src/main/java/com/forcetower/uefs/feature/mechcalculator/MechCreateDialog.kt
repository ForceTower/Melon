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

package com.forcetower.uefs.feature.mechcalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.DialogCreateMechValueBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import timber.log.Timber
import javax.inject.Inject

class MechCreateDialog : RoundedDialog(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    lateinit var viewModel: MechanicalViewModel
    lateinit var binding: DialogCreateMechValueBinding

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        return DialogCreateMechValueBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textInputGrade.run {
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && isEnabled) {
                    val string = text.toString()
                    if (string.isNotBlank()) {
                        val parsed = string.replace(",", ".").toDoubleOrNull()
                        if (parsed == null) {
                            error = context.getString(R.string.field_must_be_a_number)
                        } else {
                            error = null
                        }
                    }
                }
            }
        }

        binding.textInputWeight.run {
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val string = text.toString()
                    if (string.isNotBlank()) {
                        val parsed = string.replace(",", ".").toDoubleOrNull()
                        if (parsed == null) {
                            error = context.getString(R.string.field_must_be_a_number)
                        } else {
                            error = null
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