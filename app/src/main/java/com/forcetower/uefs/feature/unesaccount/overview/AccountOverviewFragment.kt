package com.forcetower.uefs.feature.unesaccount.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
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
}