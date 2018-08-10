package com.forcetower.unes.feature.shared

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders

inline fun <reified VM: ViewModel> Fragment.provideViewModel(viewModelFactory: ViewModelProvider.Factory) =
        ViewModelProviders.of(this, viewModelFactory)[VM::class.java]