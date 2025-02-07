package dev.forcetower.unes.enrollment.ui

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.forcetower.uefs.core.injection.dependencies.EnrollmentModuleDependencies
import com.forcetower.uefs.feature.shared.UActivity
import com.google.android.play.core.splitcompat.SplitCompat
import dagger.hilt.android.EntryPointAccessors
import dev.forcetower.unes.enrollment.di.DaggerEnrollmentComponent
import javax.inject.Inject

class EnrollmentActivity : UActivity() {
    @Inject lateinit var factory: ViewModelProvider.Factory
    init { initDependencyInjection() }

    private fun initDependencyInjection() {
        addOnContextAvailableListener { ctx ->
            DaggerEnrollmentComponent.builder()
                .context(ctx)
                .dependencies(
                    EntryPointAccessors.fromActivity(
                        this,
                        EnrollmentModuleDependencies::class.java
                    )
                )
                .build()
                .inject(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SplitCompat.install(this)

        setContentView {

        }
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = factory
}