package com.forcetower.uefs.feature.unesaccount.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.databinding.FragmentServiceAccountAccessLoginBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginAccountFragment : UFragment() {
    private lateinit var binding: FragmentServiceAccountAccessLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentServiceAccountAccessLoginBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}