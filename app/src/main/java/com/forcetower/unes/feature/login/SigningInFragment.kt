package com.forcetower.unes.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.databinding.FragmentSigningInBinding
import com.forcetower.unes.feature.shared.UFragment
import timber.log.Timber

class SigningInFragment : UFragment(), Injectable {
    private lateinit var binding: FragmentSigningInBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentSigningInBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        doLogin()
    }

    private fun doLogin() {
        val username = arguments?.getString("username")
        val password = arguments?.getString("password")

        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            showSnack(getString(R.string.error_invalid_credentials))
            view?.findNavController()?.popBackStack()
        } else {
            Timber.d("UNES will perform login using $username and $password")
        }
    }
}