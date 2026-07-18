package dev.forcetower.unes.ui.feature.onboarding.intro

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.Screens
import javax.inject.Inject

@HiltViewModel
internal class IntroViewModel @Inject constructor(
    analytics: Analytics,
) : ViewModel() {

    init {
        analytics.screen(Screens.INTRO)
    }
}
