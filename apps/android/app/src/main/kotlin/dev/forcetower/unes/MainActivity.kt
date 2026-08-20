package dev.forcetower.unes

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.theme.ThemeMode
import dev.forcetower.unes.theme.ThemePreferenceStore
import dev.forcetower.unes.firebase.PushSyncCoordinator
import dev.forcetower.unes.review.ReviewPrompter
import dev.forcetower.unes.ui.feature.connected.DeepLinkHandler
import dev.forcetower.unes.ui.navigation.AppNavHost
import dev.forcetower.unes.update.InAppUpdater
import javax.inject.Inject
import kotlinx.coroutines.launch

// FragmentActivity (vs the leaner ComponentActivity) is required so the
// settings credential card can host an `androidx.biometric.BiometricPrompt` —
// the prompt is implemented as a fragment under the hood.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    internal lateinit var themePreferences: ThemePreferenceStore

    @Inject
    internal lateinit var deepLinks: DeepLinkHandler

    @Inject
    internal lateinit var pushSync: PushSyncCoordinator

    @Inject
    internal lateinit var appUpdater: InAppUpdater

    @Inject
    internal lateinit var reviewPrompter: ReviewPrompter

    private val updateFlowLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        lifecycleScope.launch { appUpdater.onUpdateFlowResult(result.resultCode) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Only on a genuinely fresh launch — after a config change the same
        // intent is re-delivered and must not re-navigate or re-refresh.
        if (savedInstanceState == null) consumeLaunchIntent(intent)
        // Unconditional (no savedInstanceState gate): `InAppUpdater` dedupes
        // per process, which keeps the downloaded-while-dead recovery alive
        // across process-death restores.
        lifecycleScope.launch { appUpdater.checkOnLaunch(updateFlowLauncher) }
        // RESUMED, not STARTED: Play needs a foreground activity.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                reviewPrompter.requests.collect { trigger ->
                    reviewPrompter.present(this@MainActivity, trigger)
                }
            }
        }
        // Dissolve the OS splash into the in-app Compose splash instead of
        // cutting: both share the same dark base color, so fading the OS layer
        // out (with the icon lifting slightly) reads as one continuous handoff.
        splashScreen.setOnExitAnimationListener { splashView ->
            val fade = ObjectAnimator.ofFloat(splashView.view, View.ALPHA, 1f, 0f)
            val iconLift = ObjectAnimator.ofFloat(splashView.iconView, View.SCALE_X, 1f, 1.06f)
            val iconLiftY = ObjectAnimator.ofFloat(splashView.iconView, View.SCALE_Y, 1f, 1.06f)
            AnimatorSet().apply {
                playTogether(fade, iconLift, iconLiftY)
                duration = 280L
                interpolator = AccelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) = splashView.remove()
                })
                start()
            }
        }
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

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { appUpdater.resumeIfStalled(updateFlowLauncher) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeLaunchIntent(intent)
    }

    private fun consumeLaunchIntent(intent: Intent?) {
        offerDeepLink(intent)
        // A tap on a mirror-relevant FCM notification lands the push's `kind`
        // in the extras (spec 0008) — fresh evidence the mirror moved. The
        // ON_START refresh already covers most tap-opens; this catches a tap
        // delivered to the already-resumed activity (shade pulled down over
        // the open app), where onNewIntent fires without an ON_START.
        if (intent?.getStringExtra("kind") != null) pushSync.request()
        // Grade pushes are the ones carrying a discipline code (docs/deeplinks.md).
        if (intent?.getStringExtra("disciplineCode") != null) reviewPrompter.noteGradePushLaunch()
    }

    // Two carriers: a real VIEW intent puts the URL in `data` (links, adb),
    // while a tap on a backend-built FCM notification launches us with the
    // push's `data` map flattened into extras — the URL rides the `url` key.
    private fun offerDeepLink(intent: Intent?) {
        val url = intent?.dataString ?: intent?.getStringExtra("url") ?: return
        deepLinks.offer(url)
    }
}
