package dev.forcetower.unes.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Nav3 routes are typed `@Serializable` keys instead of string paths. Each
// destination object/data class is a key the back stack stores directly.
sealed interface AppRoute : NavKey {
    @Serializable data object Splash : AppRoute
    @Serializable data object Welcome : AppRoute
    @Serializable data object Intro : AppRoute
    @Serializable data object Login : AppRoute
    @Serializable data class Sync(val userId: String) : AppRoute
    @Serializable data class Ready(val userName: String) : AppRoute
    @Serializable data object Home : AppRoute
}
