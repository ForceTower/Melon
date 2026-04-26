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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data object SplashState : UiState

sealed interface SplashIntent : UiIntent

sealed interface SplashEffect : UiEffect {
    data object GoHome : SplashEffect
    data object GoOnboarding : SplashEffect
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionStore: SessionStore,
) : MviViewModel<SplashState, SplashIntent, SplashEffect>(SplashState) {

    init {
        viewModelScope.launch {
            // Wait for the session store to settle (init is synchronous today,
            // but using `first` keeps us safe if the store ever loads async).
            val authState = sessionStore.authState.first()
            emitEffect(
                when (authState) {
                    is AuthState.Authenticated -> SplashEffect.GoHome
                    AuthState.Unauthenticated -> SplashEffect.GoOnboarding
                },
            )
        }
    }

    override fun onIntent(intent: SplashIntent) = Unit
}
