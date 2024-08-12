package com.forcetower.uefs.feature.unesaccount.email

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.databinding.FragmentServiceAccountLinkEmailBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LinkEmailAccountFragment : UFragment() {
    private lateinit var binding: FragmentServiceAccountLinkEmailBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentServiceAccountLinkEmailBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}