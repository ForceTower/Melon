package com.forcetower.unes.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.unes.R
import com.forcetower.unes.databinding.FragmentLoginBinding
import com.forcetower.unes.feature.shared.UFragment

class LoginFragment : UFragment() {
    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentLoginBinding.inflate(inflater, container, false).also {
            binding = it
            binding.btnNoAccount.setOnClickListener {_ ->
                showSnack(getString(R.string.there_is_no_vestibular_anymore))
            }
        }.root
    }
}