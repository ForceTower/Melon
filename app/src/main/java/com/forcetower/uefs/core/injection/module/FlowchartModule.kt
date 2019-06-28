package com.forcetower.uefs.core.injection.module

import com.forcetower.uefs.feature.flowchart.FlowchartFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FlowchartModule {
    @ContributesAndroidInjector
    abstract fun initial(): FlowchartFragment
}
