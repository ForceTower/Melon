package com.forcetower.uefs_2.feature

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs_2.databinding.FragmentDashboardBinding
import com.forcetower.uefs_2.feature.shared.UFragment

class DashboardFragment : UFragment() {
    private lateinit var binding: FragmentDashboardBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentDashboardBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}