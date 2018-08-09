package com.forcetower.uefs_2.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs_2.databinding.FragmentLoginBinding
import com.forcetower.uefs_2.feature.shared.UFragment

class LoginFragment : UFragment() {
    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentLoginBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}