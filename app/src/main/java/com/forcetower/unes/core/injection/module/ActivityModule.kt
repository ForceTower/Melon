package com.forcetower.unes.core.injection.module

import com.forcetower.unes.feature.home.HomeActivity
import com.forcetower.unes.feature.login.LoginActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityModule {
    @ContributesAndroidInjector(modules = [LoginModule::class])
    abstract fun bindLoginActivity(): LoginActivity
    @ContributesAndroidInjector(modules = [HomeModule::class])
    abstract fun bindHomeActivity() : HomeActivity
}