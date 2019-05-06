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

package com.forcetower.uefs.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import androidx.navigation.ActivityNavigator
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentLoginFormBinding
import com.forcetower.uefs.feature.shared.UFragment

class LoginFragment : UFragment() {
    private lateinit var binding: FragmentLoginFormBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

        if (password.isBlank() || password.length < 3) {
            binding.editPass.error = getString(R.string.error_too_small)
            binding.editPass.requestFocus()
            error = true
        }

        if (error) return

        // val extras = FragmentNavigatorExtras(binding.imageUnes to getString(R.string.user_image_transition))

        findNavController().navigate(R.id.action_login_form_to_signing_in, bundleOf(
                "username" to username,
                "password" to password))
        // ), null, extras)
    }

    private fun toAbout() {
        val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity())
        val extras = ActivityNavigator.Extras.Builder()
                .setActivityOptions(bundle)
                .build()
        findNavController().navigate(R.id.action_login_open_about, null, null, extras)
    }
}