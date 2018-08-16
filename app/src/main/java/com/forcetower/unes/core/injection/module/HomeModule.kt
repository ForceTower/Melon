package com.forcetower.unes.core.injection.module

import com.forcetower.unes.feature.messages.SagresMessagesFragment
import com.forcetower.unes.feature.messages.UnesMessagesFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class HomeModule {
    @ContributesAndroidInjector
    abstract fun sagresMessageFragment(): SagresMessagesFragment

    @ContributesAndroidInjector
    abstract fun unesMessageFragment(): UnesMessagesFragment
}
