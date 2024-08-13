package com.forcetower.uefs.feature.unesaccount.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentServiceAccountAccessLoginBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.unesaccount.login.vm.LoginAccountEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginAccountFragment : UFragment() {
    private lateinit var binding: FragmentServiceAccountAccessLoginBinding
    private val viewModel by viewModels<LoginAccountViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentServiceAccountAccessLoginBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.event.observe(viewLifecycleOwner, ::onEvent)

        binding.btnContinue.setOnClickListener {
            viewModel.anonymousLogin()
        }
    }

    private fun onEvent(event: LoginAccountEvent) {
        when (event) {
            LoginAccountEvent.LoginFailed -> onLoginFailed()
            LoginAccountEvent.LoginSuccess -> onLoginCompleted()
        }
    }

    private fun onLoginCompleted() {
        val direction = LoginAccountFragmentDirections.actionUnesAccountLoginToLinkEmailAccountFragment()
        findNavController().navigate(direction)
    }

    private fun onLoginFailed() {
        showSnack(getString(R.string.service_account_login_anonymous_failed))
    }
}