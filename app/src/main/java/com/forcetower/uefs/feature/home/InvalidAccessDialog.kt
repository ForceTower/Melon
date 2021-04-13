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

package com.forcetower.uefs.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.databinding.DialogInvalidAccessBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InvalidAccessDialog : RoundedDialog() {
    private lateinit var binding: DialogInvalidAccessBinding
    private val viewModel: HomeViewModel by activityViewModels()

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogInvalidAccessBinding.inflate(inflater, container, false).also {
            binding = it
            it.btnCancel.setOnClickListener {
                viewModel.changeAccessValidation(true)
                dismiss()
            }
            it.btnChange.setOnClickListener {
                preparePassword()
            }
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.passwordChangeProcess.observe(
            viewLifecycleOwner,
            EventObserver {
                if (it.status == Status.SUCCESS) {
                    binding.run {
                        btnChange.isEnabled = true
                        btnCancel.isEnabled = true
                        pbOperation.visibility = INVISIBLE
                    }

                    if (it.data == true) {
                        viewModel.showSnack(getString(R.string.invalid_access_password_change))
                        dismiss()
                    } else if (it.data == false) {
                        binding.etPassword.run {
                            error = getString(R.string.invalid_access_new_password_is_incorrect)
                            requestFocus()
                        }
                    }
                } else if (it.status == Status.LOADING) {
                    binding.run {
                        btnChange.isEnabled = false
                        btnCancel.isEnabled = false
                        pbOperation.visibility = VISIBLE
                    }
                } else {
                    dismiss()
                }
            }
        )
    }

    private fun preparePassword() {
        val password = binding.etPassword.text.toString()
        if (password.length < 3) {
            binding.etPassword.run {
                error = getString(R.string.error_too_small)
                requestFocus()
            }
            return
        }

        viewModel.attemptNewPasswordLogin(password)
    }
}
