package com.forcetower.uefs.feature.unesaccount.email

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentServiceAccountLinkEmailBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.unesaccount.email.vm.LinkEmailAccountEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LinkEmailAccountFragment : UFragment() {
    private lateinit var binding: FragmentServiceAccountLinkEmailBinding
    private val viewModel by viewModels<LinkEmailAccountViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentServiceAccountLinkEmailBinding.inflate(inflater, container, false).also {
            binding = it
            binding.viewModel = viewModel
            binding.lifecycleOwner = viewLifecycleOwner
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner, ::onEvent)

        binding.btnContinue.setOnClickListener {
            val email = binding.email.text?.toString() ?: return@setOnClickListener
            viewModel.link(email)
        }
    }

    private fun onEvent(event: LinkEmailAccountEvent) {
        when (event) {
            is LinkEmailAccountEvent.EmailSent -> onEmailSent(event.token, event.email)
            LinkEmailAccountEvent.InvalidEmail -> onInvalidEmail()
            LinkEmailAccountEvent.InvalidInfo -> onInvalidInfo()
            LinkEmailAccountEvent.SendError -> onSendError()
        }
    }

    private fun onEmailSent(token: String, email: String) {
        val directions = LinkEmailAccountFragmentDirections.actionUnesAccountLinkEmailToUnesAccountConfirmEmail(token, email)
        findNavController().navigate(directions)
    }

    private fun onInvalidEmail() {
        showSnack(getString(R.string.service_account_email_invalid_format))
    }

    private fun onInvalidInfo() {
        showSnack(getString(R.string.service_account_email_send_invalid_info))
    }

    private fun onSendError() {
        showSnack(getString(R.string.service_account_email_send_failed))
    }
}