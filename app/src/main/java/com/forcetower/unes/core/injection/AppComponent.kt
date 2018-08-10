package com.forcetower.unes.core.injection

import com.forcetower.unes.UApplication
import com.forcetower.unes.core.injection.module.AppModule
import com.forcetower.unes.core.injection.module.NetworkModule
import com.forcetower.unes.core.injection.module.ViewModelModule
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        AndroidSupportInjectionModule::class,
        AppModule::class,
        NetworkModule::class,
        ViewModelModule::class
    ]
)
interface AppComponent: AndroidInjector<UApplication> {
    @Component.Builder
    abstract class Builder: AndroidInjector.Builder<UApplication>()
}