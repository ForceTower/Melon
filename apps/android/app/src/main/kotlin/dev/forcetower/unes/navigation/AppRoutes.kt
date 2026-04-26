package dev.forcetower.unes.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Nav3 routes are typed `@Serializable` keys instead of string paths. Each
// destination object/data class is a key the back stack stores directly.
sealed interface AppRoute : NavKey {
    @Serializable data object Home : AppRoute
}
