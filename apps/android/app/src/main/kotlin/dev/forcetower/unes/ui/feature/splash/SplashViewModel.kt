package dev.forcetower.unes.ui.feature.splash

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Minimum time the splash stays up before flowing into Welcome — only for
// users who still need onboarding. Mirrors iOS OnboardingFeature (2.6s).
internal const val SplashHoldMillis = 2_600L

// State, not a one-shot effect: a splash recreated mid-hold (rotation, theme
// switch) must still find out where to go.
internal enum class SplashState : UiState {
    Holding,
    GoHome,
    GoOnboarding,
}

internal sealed interface SplashIntent : UiIntent

internal sealed interface SplashEffect : UiEffect

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val sessionStore: SessionStore,
) : MviViewModel<SplashState, SplashIntent, SplashEffect>(SplashState.Holding) {

    // A returning user goes home as soon as the session is known (same as iOS
    // RootFeature.bootstrap); only the onboarding path waits out the hold.
    init {
        viewModelScope.launch {
            val hold = launch { delay(SplashHoldMillis) }
            val next = when (sessionStore.currentAuthState()) {
                is AuthState.Authenticated -> {
                    hold.cancel()
                    SplashState.GoHome
                }
                AuthState.Unauthenticated -> {
                    hold.join()
                    SplashState.GoOnboarding
                }
            }
            setState { next }
        }
    }

    override fun onIntent(intent: SplashIntent) = Unit
}
