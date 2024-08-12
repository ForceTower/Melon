package com.forcetower.uefs.feature.unesaccount.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.databinding.FragmentServiceAccountLinkEmailConfirmationBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmEmailAccountFragment : UFragment() {
    private lateinit var binding: FragmentServiceAccountLinkEmailConfirmationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentServiceAccountLinkEmailConfirmationBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}