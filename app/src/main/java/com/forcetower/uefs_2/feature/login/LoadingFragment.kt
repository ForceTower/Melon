package com.forcetower.uefs_2.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs_2.databinding.FragmentLoadingBinding
import com.forcetower.uefs_2.feature.shared.UFragment

class LoadingFragment : UFragment() {
    private lateinit var binding: FragmentLoadingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentLoadingBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}