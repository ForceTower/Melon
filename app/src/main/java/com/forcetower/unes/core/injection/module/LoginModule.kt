package com.forcetower.unes.core.injection.module

import com.forcetower.unes.feature.login.LoadingFragment
import com.forcetower.unes.feature.login.SigningInFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class LoginModule {
    @ContributesAndroidInjector
    abstract fun bindLoadingFragment(): LoadingFragment
    @ContributesAndroidInjector
    abstract fun bindSigningInFragment(): SigningInFragment
}
