package com.forcetower.uefs.feature.universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.core.vm.UnesverseViewModel
import com.forcetower.uefs.databinding.FragmentUniverseRequiredBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import javax.inject.Inject

class UnesverseRequiredFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var binding: FragmentUniverseRequiredBinding
    private lateinit var viewModel: UnesverseViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        return FragmentUniverseRequiredBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            btnConnect.setOnClickListener { connect() }
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.connecting = viewModel.isLoggingIn
        viewModel.loginMessenger.observe(this, EventObserver {
            val message = getString(it)
            showSnack(message)
        })
        viewModel.access.observe(this, Observer {
            if (it != null) {
                findNavController().navigate(R.id.action_unesverse_required_to_presentation)
            }
        })
    }

    private fun connect() {
        viewModel.login()
    }
}