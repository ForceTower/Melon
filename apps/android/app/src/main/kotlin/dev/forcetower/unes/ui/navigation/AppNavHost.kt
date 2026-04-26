package dev.forcetower.unes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.forcetower.unes.ui.feature.home.HomeScreen
import dev.forcetower.unes.ui.feature.onboarding.intro.IntroCarouselScreen
import dev.forcetower.unes.ui.feature.onboarding.login.LoginScreen
import dev.forcetower.unes.ui.feature.onboarding.ready.ReadyScreen
import dev.forcetower.unes.ui.feature.splash.SplashScreen
import dev.forcetower.unes.ui.feature.onboarding.sync.SyncScreen
import dev.forcetower.unes.ui.feature.onboarding.welcome.WelcomeScreen

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(AppRoute.Splash)

    fun replace(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<AppRoute.Splash> {
                SplashScreen(onDone = { replace(AppRoute.Welcome) })
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
                    onSubmit = { id -> replace(AppRoute.Sync(userId = id)) },
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            entry<AppRoute.Sync> { route ->
                SyncScreen(
                    userId = route.userId,
                    onDone = { replace(AppRoute.Ready(userName = route.userId)) },
                )
            }
            entry<AppRoute.Ready> { route ->
                ReadyScreen(
                    userName = route.userName,
                    onEnter = { replace(AppRoute.Home) },
                )
            }
            entry<AppRoute.Home> { HomeScreen() }
        },
    )
}
