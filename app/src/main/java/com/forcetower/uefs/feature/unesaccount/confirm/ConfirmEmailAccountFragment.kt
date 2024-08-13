package com.forcetower.uefs.feature.unesaccount.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.core.extensions.closeKeyboard
import com.forcetower.core.extensions.openKeyboard
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentServiceAccountLinkEmailConfirmationBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.unesaccount.confirm.vm.ConfirmEmailAccountEvent
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmEmailAccountFragment : UFragment() {
    private lateinit var binding: FragmentServiceAccountLinkEmailConfirmationBinding
    private val viewModel by viewModels<ConfirmEmailAccountViewModel>()
    private val args by navArgs<ConfirmEmailAccountFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentServiceAccountLinkEmailConfirmationBinding.inflate(inflater, container, false).also {
            binding = it
            binding.email = args.email
            binding.viewModel = viewModel
            binding.lifecycleOwner = viewLifecycleOwner
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner, ::onEvent)

        binding.btnContinue.setOnClickListener {
            val code = binding.code.text?.toString() ?: return@setOnClickListener
            viewModel.submit(code, args.securityCode)
            binding.btnContinue.closeKeyboard()
        }

        binding.btnResend.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun onEvent(event: ConfirmEmailAccountEvent) {
        when (event) {
            ConfirmEmailAccountEvent.Completed -> onCompleted()
            ConfirmEmailAccountEvent.ConnectionFailed -> onConnectionFailed()
            ConfirmEmailAccountEvent.EmailTaken -> onEmailTaken()
            ConfirmEmailAccountEvent.InvalidCode -> onInvalidCode()
            ConfirmEmailAccountEvent.TooManyTries -> onTooManyTries()
        }
    }

    private fun onCompleted() {
        showSnack(getString(R.string.service_account_email_confirm_linked))
        findNavController().popBackStack(R.id.unes_account_overview, false)
    }

    private fun onConnectionFailed() {
        showSnack(getString(R.string.service_account_email_confirm_connection_failed))
    }

    private fun onEmailTaken() {
        showSnack(getString(R.string.service_account_email_confirm_email_taken), Snackbar.LENGTH_LONG)
    }

    private fun onInvalidCode() {
        binding.codeLayout.error = getString(R.string.service_account_email_confirm_invalid_code)
        binding.codeLayout.openKeyboard()
    }

    private fun onTooManyTries() {
        showSnack(getString(R.string.service_account_email_confirm_too_many_tries), Snackbar.LENGTH_LONG)
    }
}