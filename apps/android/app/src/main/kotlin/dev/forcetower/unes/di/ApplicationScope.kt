package dev.forcetower.unes.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ApplicationScope

// Process-lifetime coroutine scope for fire-and-forget work that must outlive
// any individual ViewModel (e.g. background pagination kicked off during
// onboarding sync). Replaces ad-hoc GlobalScope usage so cancellation,
// dispatcher choice, and exception handling stay consistent across features.
// SupervisorJob keeps a failing child from cancelling siblings; Dispatchers
// default to compute-bound — call sites switch to IO when they need it.
@Module
@InstallIn(SingletonComponent::class)
internal object ApplicationScopeModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
