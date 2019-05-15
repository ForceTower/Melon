package com.forcetower.uefs.feature.evaluation

import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import javax.inject.Inject

class InitialFragment : UFragment(), Injectable {
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: EvaluationViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val onboarding = preferences.getBoolean("completed_evaluation_onboarding", false)
        viewModel = provideActivityViewModel(factory)
        viewModel.getToken().observe(this, Observer {
            if (it == null) {
                findNavController().navigate(R.id.action_initial_to_unesverse_required)
            } else if (!onboarding) {
                findNavController().navigate(R.id.action_initial_to_presentation)
            } else {
                findNavController().navigate(R.id.action_initial_to_home)
            }
        })
    }
}