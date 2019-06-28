package com.forcetower.uefs.feature.flowchart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentFlowchartBinding
import com.forcetower.uefs.feature.shared.UFragment
import javax.inject.Inject

class FlowchartFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var binding: FragmentFlowchartBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentFlowchartBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }
}