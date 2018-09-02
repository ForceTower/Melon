/*
 * Copyright (c) 2018.
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