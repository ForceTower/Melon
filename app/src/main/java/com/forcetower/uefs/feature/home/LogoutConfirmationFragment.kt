package com.forcetower.uefs.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentLogoutConfirmationBinding
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import javax.inject.Inject

class LogoutConfirmationFragment : BottomSheetDialogFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: HomeViewModel
    private lateinit var binding: FragmentLogoutConfirmationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.UTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        viewModel = provideActivityViewModel(factory)
        return FragmentLogoutConfirmationBinding.inflate(themedInflater, container, false).also {
            binding = it
        }.apply {
            btnCancel.setOnClickListener { dismiss() }
            btnConfirm.setOnClickListener { viewModel.logout(); dismiss() }
        }.root
    }
}