package dev.forcetower.unes.ui.feature.foliorunner

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.Screens
import javax.inject.Inject

@HiltViewModel
internal class FolioRunnerViewModel @Inject constructor(
    analytics: Analytics,
) : ViewModel() {

    init {
        analytics.screen(Screens.FOLIO_RUNNER)
    }
}
