package com.forcetower.uefs.feature.unesaccount.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.databinding.FragmentServiceAccountOverviewBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountOverviewFragment : UFragment() {
    private lateinit var binding: FragmentServiceAccountOverviewBinding
    private val viewModel by viewModels<AccountOverviewViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.fetch()
        return FragmentServiceAccountOverviewBinding.inflate(inflater, container, false).also {
            binding = it
            binding.viewModel = viewModel
            binding.lifecycleOwner = viewLifecycleOwner
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            onLoginStart()
        }

        binding.btnAddEmail.setOnClickListener {
            onLinkEmail()
        }
    }

    private fun onLoginStart() {
        val directions = AccountOverviewFragmentDirections.actionUnesAccountOverviewToUnesAccountStart()
        findNavController().navigate(directions)
    }

    private fun onLinkEmail() {
        val directions = AccountOverviewFragmentDirections.actionUnesAccountOverviewToUnesAccountLinkEmail()
        findNavController().navigate(directions)
    }
}