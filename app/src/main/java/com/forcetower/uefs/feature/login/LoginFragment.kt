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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ActivityNavigator
import androidx.navigation.fragment.findNavController
import com.forcetower.sagres.Constants
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.edge.auth.RegisterPasskeyStart
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.databinding.FragmentLoginFormBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : UFragment() {
    @Inject lateinit var remoteConfig: FirebaseRemoteConfig
    @Inject lateinit var preferences: SharedPreferences

    private val viewModel by viewModels<LoginFormViewModel>()
    private lateinit var binding: FragmentLoginFormBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        allowOnlyUEFS()
        return FragmentLoginFormBinding.inflate(inflater, container, false).also {
            binding = it
            binding.btnInstitution.setOnClickListener {
                showInstitutionSelector()
            }
            binding.btnConnect.setOnClickListener {
                prepareLogin()
            }
            binding.btnAboutUnes.setOnClickListener {
                toAbout()
            }
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.challenge.observe(viewLifecycleOwner) {
            startCredentialsManager(it)
        }

        viewModel.register.observe(viewLifecycleOwner) {
            runCatching {
                startPasskeyRegister(it)
            }
        }
    }

    private fun startPasskeyRegister(start: RegisterPasskeyStart) {
        val manager = CredentialManager.create(requireContext())

        Timber.d("Register challenge: ${start.create}")

        val request = CreatePublicKeyCredentialRequest(
            requestJson = start.create,
            preferImmediatelyAvailableCredentials = false,
        )

        lifecycleScope.launch {
            try {
                val result = manager.createCredential(
                    context = requireActivity(),
                    request = request,
                )
                handlePasskeyRegistrationResult(result, start)
            } catch (e: CreateCredentialException) {
                Timber.e(e, "Failed to create passkey")
                showSnack("Não foi possível recuperar credenciais.")
            }
        }
    }

    private fun handlePasskeyRegistrationResult(
        result: CreateCredentialResponse,
        start: RegisterPasskeyStart
    ) {
        if (result is CreatePublicKeyCredentialResponse) {
            Timber.d("Register Response: ${result.registrationResponseJson}")
            viewModel.finishRegister(start.flowId, result.registrationResponseJson)
        } else {
            Timber.e("This is not a passkey. lol")
        }
    }

    private fun startCredentialsManager(challenge: String) {
        val manager = CredentialManager.create(requireContext())
        val passwordOption = GetPasswordOption()

        val publicKeyCredential = GetPublicKeyCredentialOption(
            requestJson = challenge
        )

        val request = GetCredentialRequest(listOf(passwordOption, publicKeyCredential))

        lifecycleScope.launch {
            try {
                val result = manager.getCredential(
                    context = requireActivity(),
                    request = request
                )
                onCredentialSignInCompleted(result)
            } catch (e: GetCredentialException) {
                Timber.e(e, "Failed to get credentials")
                showSnack("Não foi possível recuperar credenciais.")
            }
        }
    }

    private fun onCredentialSignInCompleted(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is PublicKeyCredential -> {
                val responseJson = credential.authenticationResponseJson
                viewModel.completeAssertion(responseJson)
            }

            is PasswordCredential -> {
                val username = credential.id
                val password = credential.password
                doLogin(username, password)
            }
        }
    }

    private fun showInstitutionSelector() {
        val dialog = InstitutionSelectDialog()
        dialog.show(childFragmentManager, "dialog_institution_selector")
    }

    private fun prepareLogin() {
        val username = binding.editUser.text.toString()
        val password = binding.editPass.text.toString()
        var error = false

        if (username.isBlank() || username.length < 3) {
            binding.editUser.error = getString(R.string.error_too_small)
            binding.editUser.requestFocus()
            error = true
        }

        if (password.isBlank() || password.length < 2) {
            binding.editPass.error = getString(R.string.error_too_small)
            binding.editPass.requestFocus()
            error = true
        }

        if (error) return

        doLogin(username, password)
    }

    private fun doLogin(username: String, password: String) {
        val snowpiercer =
            preferences.isStudentFromUEFS() && remoteConfig.getBoolean("feature_flag_use_snowpiercer")
        FirebaseCrashlytics.getInstance().setCustomKey("snowpiercer_user", snowpiercer)
        val info = bundleOf(
            "username" to username,
            "password" to password,
            "snowpiercer" to snowpiercer
        )

        if (Constants.getParameter("REQUIRES_CAPTCHA") != "true" || snowpiercer) {
            findNavController().navigate(R.id.action_login_form_to_signing_in, info)
        } else {
            val directions =
                LoginFragmentDirections.actionLoginFormToTechNopeCaptchaStuff(username, password)
            findNavController().navigate(directions)
        }
    }

    private fun loginWithPasskey() {
        viewModel.startAssertion()
    }

    private fun toAbout() {
        val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity())
        val extras = ActivityNavigator.Extras.Builder()
            .setActivityOptions(bundle)
            .build()
        findNavController().navigate(R.id.action_login_open_about, null, null, extras)
    }

    private fun allowOnlyUEFS() {
        preferences.edit()
            .putString(com.forcetower.uefs.core.constants.Constants.SELECTED_INSTITUTION_KEY, "UEFS")
            .apply()

        SagresNavigator.instance.setSelectedInstitution("UEFS")
    }
}
