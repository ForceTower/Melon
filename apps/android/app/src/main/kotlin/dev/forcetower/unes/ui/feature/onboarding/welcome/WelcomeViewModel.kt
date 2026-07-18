package dev.forcetower.unes.ui.feature.onboarding.welcome

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.analytics.Screens
import javax.inject.Inject

@HiltViewModel
internal class WelcomeViewModel @Inject constructor(
    private val analytics: Analytics,
) : ViewModel() {

    init {
        analytics.screen(Screens.WELCOME)
    }

    fun trackStart() {
        analytics.selectContent(ContentTypes.CTA, itemId = "welcome_start")
    }

    fun trackLogin() {
        analytics.selectContent(ContentTypes.CTA, itemId = "welcome_login")
    }
}
