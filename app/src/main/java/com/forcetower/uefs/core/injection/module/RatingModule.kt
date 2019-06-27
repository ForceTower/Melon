package com.forcetower.uefs.core.injection.module

import com.forcetower.uefs.feature.evaluation.rating.InternalQuestionFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class RatingModule {
    @ContributesAndroidInjector
    abstract fun internalQuestion(): InternalQuestionFragment
}