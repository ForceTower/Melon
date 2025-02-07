package dev.forcetower.unes.enrollment.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider
import com.forcetower.uefs.core.injection.dependencies.EnrollmentModuleDependencies
import com.forcetower.uefs.feature.shared.UActivity
import com.google.android.play.core.splitcompat.SplitCompat
import dagger.hilt.android.EntryPointAccessors
import dev.forcetower.unes.enrollment.di.DaggerEnrollmentComponent
import dev.forcetower.unes.enrollment.ui.onboarding.OnboardingScreen
import dev.forcetower.unes.enrollment.ui.theme.EnrollmentTheme
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
        enableEdgeToEdge()
        setContent {
            EnrollmentTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    OnboardingScreen(
                        modifier = Modifier
                            .padding(
                                top = padding.calculateTopPadding(),
                                start = padding.calculateStartPadding(LayoutDirection.Ltr),
                                end = padding.calculateEndPadding(LayoutDirection.Ltr),
                                bottom = padding.calculateBottomPadding()
                            )
                    )
                }
            }
        }
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = factory
}