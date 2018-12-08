package com.forcetower.uefs.feature.adventure

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentAdventureBeginsBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.provideActivityViewModel
import timber.log.Timber
import javax.inject.Inject

class AdventureFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: AdventureViewModel
    private var activity: UGameActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as? UGameActivity
        activity ?: Timber.e("Adventure Fragment must be attached to a UGameActivity for it to work")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        return FragmentAdventureBeginsBinding.inflate(inflater, container, false).apply {
            interactor = viewModel
        }.root
    }
}