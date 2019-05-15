package com.forcetower.uefs.feature.evaluation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.databinding.FragmentEvaluationPresentationBinding
import com.forcetower.uefs.feature.shared.UFragment

class PresentationFragment : UFragment() {
    private lateinit var binding: FragmentEvaluationPresentationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentEvaluationPresentationBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}