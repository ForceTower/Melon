package com.forcetower.unes.core.injection.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.forcetower.unes.core.injection.annotation.ViewModelKey
import com.forcetower.unes.core.vm.LoginViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    abstract fun bindLoginViewModel(vm: LoginViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: UViewModelFactory): ViewModelProvider.Factory
}