package dev.forcetower.unes.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.forcetower.unes.R
import dev.forcetower.unes.ui.feature.connected.ConnectedScreen
import dev.forcetower.unes.ui.feature.onboarding.intro.IntroCarouselScreen
import dev.forcetower.unes.ui.feature.onboarding.login.LoginScreen
import dev.forcetower.unes.ui.feature.onboarding.ready.ReadyScreen
import dev.forcetower.unes.ui.feature.onboarding.sync.SyncScreen
import dev.forcetower.unes.ui.feature.onboarding.welcome.WelcomeScreen
import dev.forcetower.unes.ui.feature.splash.SplashScreen

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(AppRoute.Splash)
    val context = LocalContext.current
    val authFailedToast = stringResource(R.string.onboarding_sync_auth_failed)

    fun replace(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = { iosPush() },
        popTransitionSpec = { iosPop() },
        predictivePopTransitionSpec = { _ -> iosPop() },
        entryProvider = entryProvider {
            entry<AppRoute.Splash> {
                SplashScreen(
                    onGoHome = { replace(AppRoute.Connected) },
                    onGoOnboarding = { replace(AppRoute.Welcome) },
                )
            }
            entry<AppRoute.Welcome> {
                WelcomeScreen(
                    onNext = { backStack.add(AppRoute.Intro) },
                    onLogin = { backStack.add(AppRoute.Login) },
                )
            }
            entry<AppRoute.Intro> {
                IntroCarouselScreen(
                    onDone = { backStack.add(AppRoute.Login) },
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            entry<AppRoute.Login> {
                LoginScreen(
                    onSubmit = { firstName -> replace(AppRoute.Sync(firstName = firstName)) },
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            entry<AppRoute.Sync> { route ->
                SyncScreen(
                    firstName = route.firstName,
                    onDone = { replace(AppRoute.Ready(firstName = route.firstName)) },
                    onAuthFailed = {
                        Toast.makeText(context, authFailedToast, Toast.LENGTH_LONG).show()
                        replace(AppRoute.Welcome)
                    },
                )
            }
            entry<AppRoute.Ready> { route ->
                ReadyScreen(
                    firstName = route.firstName,
                    onEnter = { replace(AppRoute.Connected) },
                )
            }
            entry<AppRoute.Connected> {
                ConnectedScreen(onLoggedOut = { replace(AppRoute.Welcome) })
            }
        },
    )
}
