package com.forcetower.uefs.core.injection.module

import com.forcetower.uefs.feature.flowchart.SelectCourseFragment
import com.forcetower.uefs.feature.flowchart.discipline.DisciplineFragment
import com.forcetower.uefs.feature.flowchart.home.FlowchartFragment
import com.forcetower.uefs.feature.flowchart.home.SemestersFragment
import com.forcetower.uefs.feature.flowchart.semester.SemesterFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FlowchartModule {
    @ContributesAndroidInjector
    abstract fun selector(): SelectCourseFragment
    @ContributesAndroidInjector
    abstract fun initial(): FlowchartFragment
    @ContributesAndroidInjector
    abstract fun semesters(): SemestersFragment
    @ContributesAndroidInjector
    abstract fun semester(): SemesterFragment
    @ContributesAndroidInjector
    abstract fun discipline(): DisciplineFragment
}
