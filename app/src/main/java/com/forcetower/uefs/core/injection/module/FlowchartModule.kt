package com.forcetower.uefs.core.injection.module

import com.forcetower.uefs.feature.flowchart.home.FlowchartFragment
import com.forcetower.uefs.feature.flowchart.home.SemesterFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FlowchartModule {
    @ContributesAndroidInjector
    abstract fun initial(): FlowchartFragment
    @ContributesAndroidInjector
    abstract fun semester(): SemesterFragment
}
