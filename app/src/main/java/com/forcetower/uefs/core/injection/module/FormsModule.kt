package com.forcetower.uefs.core.injection.module

import com.forcetower.uefs.feature.forms.InternalFormFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FormsModule {
    @ContributesAndroidInjector
    abstract fun internalForm(): InternalFormFragment
}