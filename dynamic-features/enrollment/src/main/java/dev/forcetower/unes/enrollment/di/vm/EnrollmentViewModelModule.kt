package dev.forcetower.unes.enrollment.di.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.forcetower.core.base.BaseViewModelFactory
import com.forcetower.core.injection.annotation.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck
import dagger.multibindings.IntoMap
import dev.forcetower.unes.enrollment.ui.onboarding.vm.OnboardingViewModel

@Module
@DisableInstallInCheck
internal interface EnrollmentViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(OnboardingViewModel::class)
    fun onboarding(vm: OnboardingViewModel): ViewModel

    @Binds
    fun bindViewModelFactory(factory: BaseViewModelFactory): ViewModelProvider.Factory
}