package dev.forcetower.unes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.ui.navigation.AppNavHost

// FragmentActivity (vs the leaner ComponentActivity) is required so the
// settings credential card can host an `androidx.biometric.BiometricPrompt` —
// the prompt is implemented as a fragment under the hood.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelonTheme {
                AppNavHost()
            }
        }
    }
}
