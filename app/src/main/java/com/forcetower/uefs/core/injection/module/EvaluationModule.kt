package com.forcetower.uefs.core.injection.module

import com.forcetower.uefs.feature.evaluation.InitialFragment
import com.forcetower.uefs.feature.evaluation.PresentationFragment
import com.forcetower.uefs.feature.evaluation.discipline.DisciplineEvaluationFragment
import com.forcetower.uefs.feature.evaluation.home.HomeFragment
import com.forcetower.uefs.feature.evaluation.rating.RatingFragment
import com.forcetower.uefs.feature.evaluation.search.SearchFragment
import com.forcetower.uefs.feature.evaluation.teacher.TeacherFragment
import com.forcetower.uefs.feature.universe.UnesverseRequiredFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class EvaluationModule {
    @ContributesAndroidInjector
    abstract fun initial(): InitialFragment
    @ContributesAndroidInjector
    abstract fun presentation(): PresentationFragment
    @ContributesAndroidInjector
    abstract fun unesverseRequired(): UnesverseRequiredFragment
    @ContributesAndroidInjector
    abstract fun home(): HomeFragment
    @ContributesAndroidInjector
    abstract fun search(): SearchFragment
    @ContributesAndroidInjector
    abstract fun teacher(): TeacherFragment
    @ContributesAndroidInjector
    abstract fun discipline(): DisciplineEvaluationFragment
    @ContributesAndroidInjector
    abstract fun rating(): RatingFragment
}