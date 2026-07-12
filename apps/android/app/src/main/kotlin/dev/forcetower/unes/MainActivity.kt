package dev.forcetower.unes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.theme.ThemeMode
import dev.forcetower.unes.theme.ThemePreferenceStore
import dev.forcetower.unes.ui.navigation.AppNavHost
import javax.inject.Inject

// FragmentActivity (vs the leaner ComponentActivity) is required so the
// settings credential card can host an `androidx.biometric.BiometricPrompt` —
// the prompt is implemented as a fragment under the hood.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    internal lateinit var themePreferences: ThemePreferenceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // System is the safe initial: the first route is the always-dark
            // splash, so the real preference lands before themed UI shows.
            val mode by themePreferences.mode.collectAsState(initial = ThemeMode.System)
            val darkTheme = when (mode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }
            MelonTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}
