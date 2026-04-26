package dev.forcetower.unes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.forcetower.unes.feature.home.HomeScreen

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(AppRoute.Home)

    NavDisplay(
        backStack = backStack,
        onBack = { count -> repeat(count) { backStack.removeLastOrNull() } },
        entryProvider = entryProvider {
            entry<AppRoute.Home> { HomeScreen() }
        },
    )
}
