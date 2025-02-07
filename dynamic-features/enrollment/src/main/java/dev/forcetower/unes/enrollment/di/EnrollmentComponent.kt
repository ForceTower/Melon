package dev.forcetower.unes.enrollment.di

import android.content.Context
import com.forcetower.uefs.core.injection.dependencies.EnrollmentModuleDependencies
import dagger.BindsInstance
import dagger.Component
import dev.forcetower.unes.enrollment.ui.EnrollmentActivity

@Component(
    modules = [],
    dependencies = [EnrollmentModuleDependencies::class]
)
interface EnrollmentComponent {
    @Component.Builder
    interface Builder {
        fun context(@BindsInstance context: Context): Builder
        fun dependencies(dependencies: EnrollmentModuleDependencies): Builder
        fun build(): EnrollmentComponent
    }

    fun inject(activity: EnrollmentActivity)
}