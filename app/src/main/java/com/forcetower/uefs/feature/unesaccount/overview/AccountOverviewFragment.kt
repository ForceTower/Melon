package com.forcetower.uefs.feature.unesaccount.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.databinding.FragmentServiceAccountOverviewBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountOverviewFragment : UFragment() {
    private lateinit var binding: FragmentServiceAccountOverviewBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentServiceAccountOverviewBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}