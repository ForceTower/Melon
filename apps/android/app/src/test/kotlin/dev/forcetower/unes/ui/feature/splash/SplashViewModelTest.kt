package dev.forcetower.unes.ui.feature.splash

import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.AuthState
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal class SplashViewModelTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aLoggedOutUserWaitsOutTheHoldBeforeOnboarding() =
        runTest {
            val vm = SplashViewModel(sessionStore(AuthState.Unauthenticated))

            advanceTimeBy(SplashHoldMillis - 1)
            assertEquals(SplashState.Holding, vm.state.value)
            advanceTimeBy(1)
            runCurrent()

            assertEquals(SplashState.GoOnboarding, vm.state.value)
        }

    @Test
    fun aRecreatedSplashStillSendsALoggedOutUserToOnboarding() =
        runTest {
            val vm = SplashViewModel(sessionStore(AuthState.Unauthenticated))

            val firstScreen = backgroundScope.launch { vm.state.collect { } }
            advanceTimeBy(200)
            firstScreen.cancel()
            advanceTimeBy(SplashHoldMillis)

            assertEquals(SplashState.GoOnboarding, vm.state.value)
        }

    @Test
    fun aReturningUserGoesHomeWithoutTheHold() =
        runTest {
            val vm = SplashViewModel(sessionStore(AuthState.Authenticated(mockk())))

            runCurrent()

            assertEquals(SplashState.GoHome, vm.state.value)
        }

    private fun sessionStore(authState: AuthState): SessionStore =
        mockk {
            coEvery { currentAuthState() } returns authState
        }
}
