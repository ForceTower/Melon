package com.forcetower.uefs.core.injection.module

import com.forcetower.uefs.easter.twofoureight.Game2048Fragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class Game2048Module {
    @ContributesAndroidInjector
    abstract fun gameFragment(): Game2048Fragment
}
