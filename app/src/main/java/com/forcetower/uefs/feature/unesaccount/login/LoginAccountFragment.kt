package com.forcetower.uefs.feature.unesaccount.login

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentServiceAccountAccessLoginBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.unesaccount.login.vm.LoginAccountEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

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
            binding.viewModel = viewModel
            binding.lifecycleOwner = viewLifecycleOwner
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.event.observe(viewLifecycleOwner, ::onEvent)

        binding.btnContinue.setOnClickListener {
            viewModel.anonymousLogin()
        }

        binding.btnContinuePasskey.setOnClickListener {
            viewModel.startPasskeyAssertion()
        }

        binding.btnContinuePasskey.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    private fun onEvent(event: LoginAccountEvent) {
        when (event) {
            LoginAccountEvent.LoginFailed -> onLoginFailed()
            LoginAccountEvent.SuccessHasEmail -> onLoginCompleted()
            LoginAccountEvent.SuccessLinkEmail -> onLinkEmail()
            is LoginAccountEvent.StartPasskeyAssertion -> onStartPasskeyAssertion(event)
        }
    }

    private fun onStartPasskeyAssertion(event: LoginAccountEvent.StartPasskeyAssertion) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val manager = CredentialManager.create(requireContext())

        val publicKeyCredential = GetPublicKeyCredentialOption(
            requestJson = event.json
        )

        val request = GetCredentialRequest(listOf(publicKeyCredential))

        lifecycleScope.launch {
            try {
                val result = manager.getCredential(
                    context = requireActivity(),
                    request = request
                )
                onCredentialSignInCompleted(result, event.flowId)
            } catch (e: GetCredentialException) {
                Timber.e(e, "Failed to get credentials")
                viewModel.completePasskeyLoading()
                onLoginFailed()
            }
        }
    }

    private fun onCredentialSignInCompleted(result: GetCredentialResponse, flowId: String) {
        when (val credential = result.credential) {
            is PublicKeyCredential -> {
                val responseJson = credential.authenticationResponseJson
                viewModel.completePasskeyAssertion(flowId, responseJson)
            }
            else -> {
                onLoginFailed()
                viewModel.completePasskeyLoading()
            }
        }
    }

    private fun onLoginCompleted() {
        findNavController().popBackStack(R.id.unes_account_overview, false)
    }

    private fun onLinkEmail() {
        val direction = LoginAccountFragmentDirections.actionUnesAccountLoginToLinkEmailAccountFragment()
        findNavController().navigate(direction)
    }

    private fun onLoginFailed() {
        showSnack(getString(R.string.service_account_login_anonymous_failed))
    }
}
