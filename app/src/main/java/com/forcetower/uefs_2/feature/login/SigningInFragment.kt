package com.forcetower.uefs_2.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs_2.databinding.FragmentSigningInBinding
import com.forcetower.uefs_2.feature.shared.UFragment

class SigningInFragment : UFragment() {
    private lateinit var binding: FragmentSigningInBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentSigningInBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}