package com.forcetower.unes.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.databinding.FragmentLoginBinding
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
}