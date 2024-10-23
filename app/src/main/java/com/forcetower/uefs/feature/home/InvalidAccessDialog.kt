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

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.sagres.Constants
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.databinding.DialogInvalidAccessBinding
import com.forcetower.uefs.feature.captcha.CaptchaResolverFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@AndroidEntryPoint
class InvalidAccessDialog : BottomSheetDialogFragment() {
    @Inject lateinit var preferences: SharedPreferences

    @Inject lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var binding: DialogInvalidAccessBinding
    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        try {
            dialog.setOnShowListener {
                val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)!!
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } catch (t: Throwable) {
            Timber.d(t, "Hum...")
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return DialogInvalidAccessBinding.inflate(inflater, container, false).also {
            binding = it
            it.btnCancel.setOnClickListener {
                viewModel.changeAccessValidation(true)
                dismiss()
            }
            it.btnChange.setOnClickListener {
                internalChecks()
            }
        }.root
    }

    private fun internalChecks() {
        val password = binding.etPassword.text.toString()
        if (password.length < 3) {
            binding.etPassword.run {
                error = getString(R.string.error_too_small)
                requestFocus()
            }
            return
        }
        val studentFromUEFS = preferences.isStudentFromUEFS()
        val snowpiercer = remoteConfig.getBoolean("feature_flag_use_snowpiercer") && studentFromUEFS
        if (Constants.getParameter("REQUIRES_CAPTCHA") != "true" || snowpiercer) {
            preparePassword(password)
        } else {
            requestCaptchaToken(password)
        }
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
                    binding.run {
                        btnChange.isEnabled = true
                        btnCancel.isEnabled = true
                        pbOperation.visibility = INVISIBLE

                        etPassword.run {
                            val message = it.message ?: getString(R.string.login_access_reconnect_not_specified)
                            error = message
                            requestFocus()
                        }
                    }
                }
            }
        )
    }

    private fun requestCaptchaToken(password: String) {
        val fragment = CaptchaResolverFragment()
        fragment.setCallback(
            object : CaptchaResolverFragment.CaptchaResolvedCallback {
                override fun onCaptchaResolved(token: String) {
                    // This here is not called on the actual "main" thread
                    Timber.d("Token received $token")
                    lifecycleScope.launchWhenCreated {
                        withContext(Dispatchers.Main) {
                            viewModel.attemptNewPasswordLogin(password, token)
                        }
                    }
                }
            }
        )

        fragment.show(childFragmentManager, "captcha_resolver")
    }

    private fun preparePassword(password: String) {
        viewModel.attemptNewPasswordLogin(password)
    }
}
