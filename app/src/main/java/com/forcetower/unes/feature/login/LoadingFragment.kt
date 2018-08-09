package com.forcetower.unes.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.unes.databinding.FragmentLoadingBinding
import com.forcetower.unes.feature.shared.UFragment

class LoadingFragment : UFragment() {
    private lateinit var binding: FragmentLoadingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentLoadingBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}