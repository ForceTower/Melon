package com.forcetower.uefs.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentLoadingBinding
import com.forcetower.uefs.feature.shared.UFragment

class LoadingFragment : UFragment() {
    private lateinit var binding: FragmentLoadingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        DataBindingUtil.inflate<FragmentLoadingBinding>(inflater, R.layout.fragment_loading, container, false).also {
            binding = it
        }.root
}