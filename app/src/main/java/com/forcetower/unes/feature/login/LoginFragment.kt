/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.feature.login

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.databinding.FragmentLoginBinding
import com.forcetower.unes.feature.about.AboutActivity
import com.forcetower.unes.feature.shared.UFragment

class LoginFragment : UFragment() {
    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentLoginBinding.inflate(inflater, container, false).also {
            binding = it
            binding.btnNoAccount.setOnClickListener{_ ->
                showSnack(getString(R.string.there_is_no_vestibular_anymore))
            }
            binding.btnConnect.setOnClickListener{_ ->
                prepareLogin()
            }
            binding.btnAboutUnes.setOnClickListener {_ ->
                toAbout()
            }
        }.root
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

        if (password.isBlank() || password.length < 3) {
            binding.editPass.error = getString(R.string.error_too_small)
            binding.editPass.requestFocus()
            error = true
        }

        if (error) return

        view?.findNavController()?.navigate(R.id.action_login_form_to_signing_in, bundleOf(
                "username" to username,
                "password" to password
        ))
    }

    private fun toAbout() {
        val intent = Intent(requireContext(), AboutActivity::class.java)
        val bundle = ActivityOptions.makeSceneTransitionAnimation(requireActivity()).toBundle()
        startActivity(intent, bundle)
    }
}