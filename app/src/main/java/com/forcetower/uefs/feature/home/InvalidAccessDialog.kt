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

package com.forcetower.uefs.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.DialogInvalidAccessBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import javax.inject.Inject

class InvalidAccessDialog : RoundedDialog(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var binding: DialogInvalidAccessBinding
    private lateinit var viewModel: HomeViewModel

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.passwordChangeProcess.observe(this, EventObserver {
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
        })
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