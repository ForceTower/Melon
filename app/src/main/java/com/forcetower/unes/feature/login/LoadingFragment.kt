package com.forcetower.unes.feature.login

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.forcetower.unes.R
import com.forcetower.unes.databinding.FragmentLoadingBinding
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.fadeIn
import com.forcetower.unes.feature.shared.fadeOut

class LoadingFragment : UFragment() {
    private lateinit var binding: FragmentLoadingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentLoadingBinding.inflate(inflater, container, false).also {
            binding = it
            binding.btnFirstSteps.setOnClickListener { v ->
                v.findNavController().navigate(R.id.action_login_loading_to_login_form) }
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
            onReceiveToken()
        }, 1000)
    }

    private fun onReceiveToken() {
        binding.btnFirstSteps.fadeIn()
        binding.contentLoading.fadeOut()
    }
}