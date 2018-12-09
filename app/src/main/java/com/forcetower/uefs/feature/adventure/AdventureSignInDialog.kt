package com.forcetower.uefs.feature.adventure

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.DialogAdventureSignInBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.provideActivityViewModel
import timber.log.Timber
import javax.inject.Inject

class AdventureSignInDialog : RoundedDialog(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: AdventureViewModel
    private lateinit var binding: DialogAdventureSignInBinding
    private var activity: UGameActivity? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        activity = context as? UGameActivity
        activity ?: Timber.e("Adventure Fragment must be attached to a UGameActivity for it to work")
    }

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        return DialogAdventureSignInBinding.inflate(inflater, container, false).apply {
            interactor = viewModel
            setLifecycleOwner(this@AdventureSignInDialog)
            executePendingBindings()
        }.also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.start.observe(this, EventObserver {
            dismiss()
            activity?.signIn()
        })
        binding.btnCancel.setOnClickListener {
            dismiss()
            findNavController().popBackStack()
        }
    }
}