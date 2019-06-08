package com.forcetower.uefs.feature.evaluation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.databinding.FragmentEvaluationHomeBinding
import com.forcetower.uefs.feature.shared.UFragment

class HomeFragment : UFragment(), Injectable {
    private lateinit var binding: FragmentEvaluationHomeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentEvaluationHomeBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}