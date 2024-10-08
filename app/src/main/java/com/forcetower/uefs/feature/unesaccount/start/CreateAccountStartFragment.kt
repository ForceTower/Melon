package com.forcetower.uefs.feature.unesaccount.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.databinding.FragmentServiceAccountCreateStartBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateAccountStartFragment : UFragment() {
    private lateinit var binding: FragmentServiceAccountCreateStartBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentServiceAccountCreateStartBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnContinue.setOnClickListener {
            val directions = CreateAccountStartFragmentDirections.actionUnesAccountStartToUnesAccountLogin()
            findNavController().navigate(directions)
        }

        binding.btnWhy.setOnClickListener {
            val directions = CreateAccountStartFragmentDirections.actionUnesAccountStartToUnesAccountReasons()
            findNavController().navigate(directions)
        }
    }
}
